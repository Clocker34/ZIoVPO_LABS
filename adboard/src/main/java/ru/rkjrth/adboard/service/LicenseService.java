package ru.rkjrth.adboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.dto.license.*;
import ru.rkjrth.adboard.entity.*;
import ru.rkjrth.adboard.repository.*;
import ru.rkjrth.adboard.security.TicketSigner;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LicenseService {

    private static final String HISTORY_CREATED = "CREATED";
    private static final String HISTORY_ACTIVATED = "ACTIVATED";
    private static final String HISTORY_RENEWED = "RENEWED";

    private final LicenseRepository licenseRepository;
    private final LicenseProductRepository productRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final UserRepository userRepository;
    private final LicenseDeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseHistoryRepository historyRepository;
    private final TicketSigner ticketSigner;

    private final long ticketLifetimeSeconds;
    private final SecureRandom random = new SecureRandom();

    public LicenseService(
            LicenseRepository licenseRepository,
            LicenseProductRepository productRepository,
            LicenseTypeRepository licenseTypeRepository,
            UserRepository userRepository,
            LicenseDeviceRepository deviceRepository,
            DeviceLicenseRepository deviceLicenseRepository,
            LicenseHistoryRepository historyRepository,
            TicketSigner ticketSigner,
            @Value("${license.ticket.lifetime-seconds:300}") long ticketLifetimeSeconds) {
        this.licenseRepository = licenseRepository;
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.historyRepository = historyRepository;
        this.ticketSigner = ticketSigner;
        this.ticketLifetimeSeconds = ticketLifetimeSeconds;
    }

    /** Создание лицензии (по диаграмме последовательности выдаётся код клиенту). */
    @Transactional
    public CreatedLicenseResponse createLicense(CreateLicenseRequest req) {
        LicenseProduct product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown product"));
        LicenseType type = licenseTypeRepository.findById(req.getTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown license type"));
        User assignee = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown user"));
        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown owner"));

        if (product.isBlocked()) {
            throw new IllegalStateException("Product is blocked");
        }

        License license = new License();
        license.setCode(generateUniqueLicenseCode());
        license.setUser(assignee);
        license.setProduct(product);
        license.setLicenseType(type);
        license.setBlocked(false);
        license.setDeviceCount(req.getDeviceCount() != null ? req.getDeviceCount() : 1);
        license.setOwner(owner);
        license.setDescription(req.getDescription());
        licenseRepository.save(license);

        LicenseHistory h = new LicenseHistory();
        h.setLicense(license);
        h.setUser(owner);
        h.setStatus(HISTORY_CREATED);
        h.setChangeDate(LocalDate.now());
        h.setDescription("License issued");
        historyRepository.save(h);

        return new CreatedLicenseResponse(license.getId(), license.getCode());
    }

    /** Активация лицензии на устройстве: первая активация задаёт период действия. */
    @Transactional
    public void activateLicense(ActivateLicenseRequest req) {
        if (req.getLicenseCode() == null || req.getLicenseCode().isBlank()) {
            throw new IllegalArgumentException("licenseCode required");
        }
        License license = licenseRepository.findByCode(req.getLicenseCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Unknown license code"));
        if (license.isBlocked()) {
            throw new IllegalStateException("License blocked");
        }
        if (license.getProduct().isBlocked()) {
            throw new IllegalStateException("Product blocked");
        }
        if (!license.getUser().getId().equals(req.getUserId())) {
            throw new IllegalArgumentException("userId does not match license assignee");
        }

        LocalDate today = LocalDate.now();
        if (license.getEndingDate() != null && license.getEndingDate().isBefore(today)) {
            throw new IllegalStateException("License expired");
        }

        String mac = normalizeMac(req.getMacAddress());
        LicenseDevice device = deviceRepository.findByMacAddressIgnoreCase(mac).orElse(null);
        User assignee = license.getUser();
        if (device == null) {
            device = new LicenseDevice();
            device.setMacAddress(mac);
            device.setName(req.getDeviceName() != null ? req.getDeviceName() : "device");
            device.setUser(assignee);
            deviceRepository.save(device);
        } else if (!device.getUser().getId().equals(assignee.getId())) {
            throw new IllegalArgumentException("Device belongs to another user");
        }

        if (!deviceLicenseRepository.findByLicenseIdAndDeviceId(license.getId(), device.getId()).isEmpty()) {
            throw new IllegalStateException("License already activated on this device");
        }

        long activeDevices = deviceLicenseRepository.countByLicense_Id(license.getId());
        if (activeDevices >= license.getDeviceCount()) {
            throw new IllegalStateException("Device limit reached for this license");
        }

        DeviceLicense link = new DeviceLicense();
        link.setLicense(license);
        link.setDevice(device);
        link.setActivationDate(today);
        deviceLicenseRepository.save(link);

        if (license.getFirstActivationDate() == null) {
            license.setFirstActivationDate(today);
            int days = license.getLicenseType().getDefaultDurationInDays();
            license.setEndingDate(today.plusDays(days));
            licenseRepository.save(license);
        }

        LicenseHistory h = new LicenseHistory();
        h.setLicense(license);
        h.setUser(assignee);
        h.setStatus(HISTORY_ACTIVATED);
        h.setChangeDate(today);
        h.setDescription("Activated on device " + device.getId());
        historyRepository.save(h);
    }

    /** Проверка лицензии — возвращает подписанный тикет. */
    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest req) throws Exception {
        if (req.getLicenseCode() == null || req.getLicenseCode().isBlank()) {
            throw new IllegalArgumentException("licenseCode required");
        }
        License license = licenseRepository.findByCode(req.getLicenseCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Unknown license code"));
        if (license.isBlocked()) {
            throw new IllegalStateException("License blocked");
        }
        String mac = normalizeMac(req.getMacAddress());
        LicenseDevice device = deviceRepository.findByMacAddressIgnoreCase(mac)
                .orElseThrow(() -> new IllegalArgumentException("Unknown device"));

        if (!device.getUser().getId().equals(license.getUser().getId())) {
            throw new IllegalArgumentException("Device does not belong to license user");
        }

        boolean linked = deviceLicenseRepository.findByLicenseIdAndDeviceId(license.getId(), device.getId()).stream()
                .findAny().isPresent();
        if (!linked) {
            throw new IllegalStateException("License not activated on this device");
        }

        LocalDate today = LocalDate.now();
        boolean validPeriod = license.getEndingDate() == null || !license.getEndingDate().isBefore(today);
        if (!validPeriod) {
            throw new IllegalStateException("License expired");
        }

        Ticket ticket = new Ticket(
                Instant.now(),
                ticketLifetimeSeconds,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                license.getUser().getId(),
                device.getId(),
                license.isBlocked());

        String sig = ticketSigner.signTicket(ticket);
        return new TicketResponse(ticket, sig);
    }

    /** Продление лицензии от текущей даты окончания (или от сегодня, если просрочена). */
    @Transactional
    public void renewLicense(RenewLicenseRequest req) {
        License license = licenseRepository.findById(req.getLicenseId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown license"));
        if (license.isBlocked()) {
            throw new IllegalStateException("License blocked");
        }

        int extendDays = req.getExtendDays() != null && req.getExtendDays() > 0
                ? req.getExtendDays()
                : license.getLicenseType().getDefaultDurationInDays();

        LocalDate today = LocalDate.now();
        LocalDate base = license.getEndingDate() == null
                ? today
                : (license.getEndingDate().isBefore(today) ? today : license.getEndingDate());
        license.setEndingDate(base.plusDays(extendDays));
        licenseRepository.save(license);

        User actor = license.getOwner();
        LicenseHistory h = new LicenseHistory();
        h.setLicense(license);
        h.setUser(actor);
        h.setStatus(HISTORY_RENEWED);
        h.setChangeDate(today);
        h.setDescription("Renewed +" + extendDays + " days");
        historyRepository.save(h);
    }

    private String normalizeMac(String mac) {
        if (mac == null || mac.isBlank()) {
            throw new IllegalArgumentException("macAddress required");
        }
        return mac.trim().toUpperCase().replace('-', ':');
    }

    private String generateUniqueLicenseCode() {
        for (int i = 0; i < 10; i++) {
            String candidate = randomSegment() + "-" + randomSegment();
            if (licenseRepository.findByCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique license code");
    }

    private String randomSegment() {
        byte[] b = new byte[6];
        random.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte value : b) {
            sb.append(String.format("%02x", value));
        }
        return sb.toString();
    }

    /** DER X.509 SPKI Base64 для клиентской проверки подписи тикета. */
    public String publicKeyDerBase64() {
        return ticketSigner.publicKeyDerBase64();
    }
}
