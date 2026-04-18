package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
}

