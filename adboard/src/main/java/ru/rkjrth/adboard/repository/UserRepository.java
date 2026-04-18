package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rkjrth.adboard.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

