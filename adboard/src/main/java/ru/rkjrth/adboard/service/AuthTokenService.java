package ru.rkjrth.adboard.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.dto.TokenPairResponse;
import ru.rkjrth.adboard.entity.SessionStatus;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.entity.UserSession;
import ru.rkjrth.adboard.repository.UserRepository;
import ru.rkjrth.adboard.repository.UserSessionRepository;
import ru.rkjrth.adboard.security.JwtTokenProvider;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthTokenService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshExpirationMs;

    public AuthTokenService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public TokenPairResponse login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            throw new BadCredentialsException("Missing credentials");
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Bad credentials");
        }
        String jti = UUID.randomUUID().toString(); //после проверки пароля в БД
        Instant expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshJti(jti);
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(expiresAt);
        userSessionRepository.save(session);

        String access = jwtTokenProvider.createAccessToken(user);
        String refresh = jwtTokenProvider.createRefreshToken(user.getUsername(), jti);
        return new TokenPairResponse(access, refresh);
    }

    @Transactional
    public TokenPairResponse refresh(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new BadCredentialsException("Missing refresh token");
        }
        Claims claims;
        try {
            claims = jwtTokenProvider.validateRefreshAndGetClaims(refreshTokenRaw);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid refresh token", e);
        }
        // JJWT 0.12: jti в payload; getId() может быть недоступен в зависимости от парсера
        String jti = claims.get("jti", String.class);
        if (jti == null || jti.isBlank()) {
            jti = claims.getId();
        }
        if (jti == null || jti.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        String subject = claims.getSubject();
        UserSession session = userSessionRepository.findByRefreshJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Unknown session"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BadCredentialsException("Refresh token reuse or revoked");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(SessionStatus.REVOKED);
            userSessionRepository.save(session);
            throw new BadCredentialsException("Refresh expired");
        }

        User user = userRepository.findByUsername(subject)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new BadCredentialsException("Invalid session");
        }

        session.setStatus(SessionStatus.REPLACED);
        userSessionRepository.save(session);

        String newJti = UUID.randomUUID().toString();
        Instant newExp = Instant.now().plusMillis(refreshExpirationMs);
        UserSession next = new UserSession();
        next.setUser(user);
        next.setRefreshJti(newJti);
        next.setStatus(SessionStatus.ACTIVE);
        next.setExpiresAt(newExp);
        userSessionRepository.save(next);

        return new TokenPairResponse(
                jwtTokenProvider.createAccessToken(user),
                jwtTokenProvider.createRefreshToken(user.getUsername(), newJti));
    }
}
