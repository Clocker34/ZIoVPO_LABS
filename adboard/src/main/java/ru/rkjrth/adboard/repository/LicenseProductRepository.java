package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.LicenseProduct;

import java.util.UUID;

public interface LicenseProductRepository extends JpaRepository<LicenseProduct, UUID> {
}
