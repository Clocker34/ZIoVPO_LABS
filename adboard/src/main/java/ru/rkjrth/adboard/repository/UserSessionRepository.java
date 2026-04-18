package ru.rkjrth.adboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rkjrth.adboard.entity.SessionStatus;
import ru.rkjrth.adboard.entity.UserSession;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    @Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.refreshJti = :jti")
    Optional<UserSession> findByRefreshJti(@Param("jti") String jti);

    Optional<UserSession> findByRefreshJtiAndStatus(String refreshJti, SessionStatus status);
}
