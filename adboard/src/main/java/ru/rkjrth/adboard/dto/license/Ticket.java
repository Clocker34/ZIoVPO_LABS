package ru.rkjrth.adboard.dto.license;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Тикет с данными о лицензии для клиента; подписывается ЭЦП ({@link TicketResponse}).
 */
@JsonPropertyOrder(alphabetic = true)
public class Ticket {

    /** Текущая дата/время сервера (UTC). */
    private Instant serverCurrentAt;

    /** Время жизни самого тикета (секунды), в течение которого клиент может считать ответ актуальным. */
    private long ticketLifetimeSeconds;

    private LocalDate licenseActivationDate;
    private LocalDate licenseExpirationDate;

    private UUID userId;
    private UUID deviceId;

    /** Флаг блокировки лицензии. */
    private boolean licenseBlocked;

    public Ticket() {
    }

    public Ticket(
            Instant serverCurrentAt,
            long ticketLifetimeSeconds,
            LocalDate licenseActivationDate,
            LocalDate licenseExpirationDate,
            UUID userId,
            UUID deviceId,
            boolean licenseBlocked) {
        this.serverCurrentAt = serverCurrentAt;
        this.ticketLifetimeSeconds = ticketLifetimeSeconds;
        this.licenseActivationDate = licenseActivationDate;
        this.licenseExpirationDate = licenseExpirationDate;
        this.userId = userId;
        this.deviceId = deviceId;
        this.licenseBlocked = licenseBlocked;
    }

    public Instant getServerCurrentAt() {
        return serverCurrentAt;
    }

    public void setServerCurrentAt(Instant serverCurrentAt) {
        this.serverCurrentAt = serverCurrentAt;
    }

    public long getTicketLifetimeSeconds() {
        return ticketLifetimeSeconds;
    }

    public void setTicketLifetimeSeconds(long ticketLifetimeSeconds) {
        this.ticketLifetimeSeconds = ticketLifetimeSeconds;
    }

    public LocalDate getLicenseActivationDate() {
        return licenseActivationDate;
    }

    public void setLicenseActivationDate(LocalDate licenseActivationDate) {
        this.licenseActivationDate = licenseActivationDate;
    }

    public LocalDate getLicenseExpirationDate() {
        return licenseExpirationDate;
    }

    public void setLicenseExpirationDate(LocalDate licenseExpirationDate) {
        this.licenseExpirationDate = licenseExpirationDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isLicenseBlocked() {
        return licenseBlocked;
    }

    public void setLicenseBlocked(boolean licenseBlocked) {
        this.licenseBlocked = licenseBlocked;
    }
}
