package ru.rkjrth.adboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rkjrth.adboard.dto.license.*;
import ru.rkjrth.adboard.service.LicenseService;

import java.util.Map;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /** Создание лицензии (администратор). */
    @PostMapping("/admin/create")
    public ResponseEntity<?> create(@RequestBody CreateLicenseRequest request) {
        try {
            return ResponseEntity.ok(licenseService.createLicense(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Продление лицензии (администратор). */
    @PostMapping("/admin/renew")
    public ResponseEntity<?> renew(@RequestBody RenewLicenseRequest request) {
        try {
            licenseService.renewLicense(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Активация лицензии на устройстве (клиент). */
    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody ActivateLicenseRequest request) {
        try {
            licenseService.activateLicense(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Проверка лицензии — тикет + ЭЦП. */
    @PostMapping("/check")
    public ResponseEntity<?> check(@RequestBody CheckLicenseRequest request) {
        try {
            return ResponseEntity.ok(licenseService.checkLicense(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not build ticket"));
        }
    }

    /** Публичный ключ RSA (X.509 SubjectPublicKeyInfo, Base64 DER) для проверки ЭЦП на клиенте. */
    @GetMapping("/signing-public-key")
    public Map<String, String> signingPublicKey() {
        return Map.of("algorithm", "RSA", "publicKeyDerBase64", licenseService.publicKeyDerBase64());
    }
}
