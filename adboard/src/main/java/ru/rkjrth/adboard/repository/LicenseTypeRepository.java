package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.LicenseType;

import java.util.UUID;

public interface LicenseTypeRepository extends JpaRepository<LicenseType, UUID> {
}
