package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.LicenseHistory;

import java.util.UUID;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, UUID> {
}
