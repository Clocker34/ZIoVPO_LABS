package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

