package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.Category;
import ru.rkjrth.adboard.entity.Listing;
import ru.rkjrth.adboard.entity.User;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByOwner(User owner);

    List<Listing> findByCategory(Category category);
}

