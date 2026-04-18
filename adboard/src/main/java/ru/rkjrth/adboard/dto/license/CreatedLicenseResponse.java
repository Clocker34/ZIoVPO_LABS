package ru.rkjrth.adboard.dto.license;

import java.util.UUID;

public class CreatedLicenseResponse {

    private UUID licenseId;
    private String licenseCode;

    public CreatedLicenseResponse(UUID licenseId, String licenseCode) {
        this.licenseId = licenseId;
        this.licenseCode = licenseCode;
    }

    public UUID getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(UUID licenseId) {
        this.licenseId = licenseId;
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public void setLicenseCode(String licenseCode) {
        this.licenseCode = licenseCode;
    }
}
