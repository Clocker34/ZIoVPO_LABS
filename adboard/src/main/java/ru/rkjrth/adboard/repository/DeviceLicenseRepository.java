package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rkjrth.adboard.entity.DeviceLicense;

import java.util.List;
import java.util.UUID;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, UUID> {

    long countByLicense_Id(UUID licenseId);

    @Query("SELECT dl FROM DeviceLicense dl WHERE dl.license.id = :licenseId AND dl.device.id = :deviceId")
    List<DeviceLicense> findByLicenseIdAndDeviceId(@Param("licenseId") UUID licenseId, @Param("deviceId") UUID deviceId);
}
