package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.Listing;
import ru.rkjrth.adboard.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByListing(Listing listing);
}

