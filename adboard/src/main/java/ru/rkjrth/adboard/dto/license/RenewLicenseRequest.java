package ru.rkjrth.adboard.dto.license;

import java.util.UUID;

public class RenewLicenseRequest {

    private UUID licenseId;
    /** Если задано — продлить на это число дней; иначе используется default_duration из типа лицензии. */
    private Integer extendDays;

    public UUID getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(UUID licenseId) {
        this.licenseId = licenseId;
    }

    public Integer getExtendDays() {
        return extendDays;
    }

    public void setExtendDays(Integer extendDays) {
        this.extendDays = extendDays;
    }
}
