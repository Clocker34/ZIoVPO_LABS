package ru.rkjrth.adboard.dto.license;

import java.util.UUID;

public class ActivateLicenseRequest {

    private String licenseCode;
    private String macAddress;
    private String deviceName;
    /** Должен совпадать с пользователем лицензии и владельцем устройства. */
    private UUID userId;

    public String getLicenseCode() {
        return licenseCode;
    }

    public void setLicenseCode(String licenseCode) {
        this.licenseCode = licenseCode;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
