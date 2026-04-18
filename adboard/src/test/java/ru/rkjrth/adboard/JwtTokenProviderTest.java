package ru.rkjrth.adboard;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.rkjrth.adboard.security.JwtTokenProvider;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void refreshTokenJtiRoundTrip() {
        String jti = UUID.randomUUID().toString();
        String token = jwtTokenProvider.createRefreshToken("user", jti);
        Claims claims = jwtTokenProvider.validateRefreshAndGetClaims(token);
        assertThat(claims.getId()).isEqualTo(jti);
        assertThat(claims.get("jti")).isNotNull();
    }
}
