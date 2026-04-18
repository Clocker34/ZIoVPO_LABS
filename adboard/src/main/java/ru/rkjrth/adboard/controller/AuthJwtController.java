package ru.rkjrth.adboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rkjrth.adboard.dto.LoginRequest;
import ru.rkjrth.adboard.dto.RefreshTokenRequest;
import ru.rkjrth.adboard.dto.TokenPairResponse;
import ru.rkjrth.adboard.service.AuthTokenService;

/**
 * Задание 5: {@code POST /auth/login}, {@code POST /auth/refresh}.
 */
@RestController
@RequestMapping("/auth")
public class AuthJwtController {

    private final AuthTokenService authTokenService;

    public AuthJwtController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authTokenService.login(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            return ResponseEntity.ok(authTokenService.refresh(request.getRefreshToken()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
