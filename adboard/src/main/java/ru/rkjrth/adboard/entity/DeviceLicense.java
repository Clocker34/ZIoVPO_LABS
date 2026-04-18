package ru.rkjrth.adboard.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "device_license", indexes = {
        @Index(name = "idx_dl_license", columnList = "license_id"),
        @Index(name = "idx_dl_device", columnList = "device_id")
})
public class DeviceLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false)
    private License license;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private LicenseDevice device;

    @Column(name = "activation_date", nullable = false)
    private LocalDate activationDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public LicenseDevice getDevice() {
        return device;
    }

    public void setDevice(LicenseDevice device) {
        this.device = device;
    }

    public LocalDate getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDate activationDate) {
        this.activationDate = activationDate;
    }
}
