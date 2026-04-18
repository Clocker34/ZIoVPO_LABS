package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.LicenseDevice;

import java.util.Optional;
import java.util.UUID;

public interface LicenseDeviceRepository extends JpaRepository<LicenseDevice, UUID> {

    Optional<LicenseDevice> findByMacAddressIgnoreCase(String macAddress);
}
