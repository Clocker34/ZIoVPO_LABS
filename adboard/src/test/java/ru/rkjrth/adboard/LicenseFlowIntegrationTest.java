package ru.rkjrth.adboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.dto.license.*;
import ru.rkjrth.adboard.entity.LicenseProduct;
import ru.rkjrth.adboard.entity.LicenseType;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.LicenseHistoryRepository;
import ru.rkjrth.adboard.repository.LicenseProductRepository;
import ru.rkjrth.adboard.repository.LicenseRepository;
import ru.rkjrth.adboard.repository.LicenseTypeRepository;
import ru.rkjrth.adboard.repository.UserRepository;
import ru.mfa.signature.SigningService;
import ru.rkjrth.adboard.service.LicenseService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LicenseFlowIntegrationTest {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private SigningService signingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseProductRepository productRepository;

    @Autowired
    private LicenseTypeRepository licenseTypeRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseHistoryRepository licenseHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User assignee;
    private User owner;
    private LicenseProduct product;
    private LicenseType licenseType;

    @BeforeEach
    void seed() {
        assignee = user("license_user_assignee");
        owner = user("license_user_owner");
        userRepository.save(assignee);
        userRepository.save(owner);

        product = new LicenseProduct();
        product.setName("IDE");
        product.setBlocked(false);
        productRepository.save(product);

        licenseType = new LicenseType();
        licenseType.setName("Pro");
        licenseType.setDefaultDurationInDays(365);
        licenseType.setDescription("annual");
        licenseTypeRepository.save(licenseType);
    }

    private User user(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@t.test");
        u.setName(username);
        u.setPasswordHash(passwordEncoder.encode("Secret1!x"));
        u.setRole(User.Role.USER);
        return u;
    }

    @Test
    void createActivateCheckRenew_verifySignature() throws Exception {
        CreateLicenseRequest create = new CreateLicenseRequest();
        create.setProductId(product.getId());
        create.setTypeId(licenseType.getId());
        create.setUserId(assignee.getId());
        create.setOwnerId(owner.getId());
        create.setDeviceCount(2);

        CreatedLicenseResponse created = licenseService.createLicense(create);
        assertThat(created.getLicenseCode()).isNotBlank();

        ActivateLicenseRequest act = new ActivateLicenseRequest();
        act.setLicenseCode(created.getLicenseCode());
        act.setMacAddress("AA:BB:CC:DD:EE:01");
        act.setDeviceName("pc-1");
        act.setUserId(assignee.getId());
        licenseService.activateLicense(act);

        CheckLicenseRequest check = new CheckLicenseRequest();
        check.setLicenseCode(created.getLicenseCode());
        check.setMacAddress("AA:BB:CC:DD:EE:01");

        TicketResponse ticketResponse = licenseService.checkLicense(check);
        assertThat(ticketResponse.getSignatureBase64()).isNotBlank();
        assertThat(signingService.verify(ticketResponse.getTicket(), ticketResponse.getSignatureBase64())).isTrue();
        assertThat(ticketResponse.getTicket().getUserId()).isEqualTo(assignee.getId());
        assertThat(ticketResponse.getTicket().isLicenseBlocked()).isFalse();

        RenewLicenseRequest renew = new RenewLicenseRequest();
        renew.setLicenseId(created.getLicenseId());
        renew.setExtendDays(30);
        licenseService.renewLicense(renew);

        var lic = licenseRepository.findById(created.getLicenseId()).orElseThrow();
        assertThat(lic.getEndingDate()).isNotNull();
        assertThat(licenseHistoryRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }
}
