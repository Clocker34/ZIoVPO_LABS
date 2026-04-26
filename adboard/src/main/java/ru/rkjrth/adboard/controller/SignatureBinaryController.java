package ru.rkjrth.adboard.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rkjrth.adboard.binary.MultipartMixedBodyBuilder;
import ru.rkjrth.adboard.dto.SignatureIdsRequest;
import ru.rkjrth.adboard.entity.MalwareSignature;
import ru.rkjrth.adboard.service.MalwareSignatureService;
import ru.rkjrth.adboard.service.SignatureBinaryExportService;

import java.time.Instant;
import java.util.List;

/**
 * Binary export API for malware signatures (multipart.md §2).
 */
@RestController
@RequestMapping("/api/binary/signatures")
public class SignatureBinaryController {

    private final MalwareSignatureService malwareSignatureService;
    private final SignatureBinaryExportService binaryExportService;

    public SignatureBinaryController(MalwareSignatureService malwareSignatureService,
                                     SignatureBinaryExportService binaryExportService) {
        this.malwareSignatureService = malwareSignatureService;
        this.binaryExportService = binaryExportService;
    }

    @GetMapping("/full")
    public ResponseEntity<byte[]> full() throws Exception {
        List<MalwareSignature> records = malwareSignatureService.getAllActual();
        MultipartMixedBodyBuilder.MultipartBody multipart = binaryExportService.buildMultipartFull(records);
        return toMultipartResponse(multipart);
    }

    @GetMapping("/increment")
    public ResponseEntity<?> increment(@RequestParam(value = "since", required = false) String sinceStr) {
        if (sinceStr == null || sinceStr.isBlank()) {
            return ResponseEntity.badRequest().body("since is required");
        }
        final Instant since;
        try {
            since = parseSince(sinceStr.trim());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid since parameter");
        }
        try {
            List<MalwareSignature> records = malwareSignatureService.getIncrement(since);
            long sinceMs = since.toEpochMilli();
            MultipartMixedBodyBuilder.MultipartBody multipart =
                    binaryExportService.buildMultipartIncrement(records, sinceMs);
            return toMultipartResponse(multipart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to build package");
        }
    }

    @PostMapping("/by-ids")
    public ResponseEntity<?> byIds(@RequestBody SignatureIdsRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.badRequest().body("ids required");
        }
        try {
            List<MalwareSignature> records = malwareSignatureService.getByIds(request.getIds());
            MultipartMixedBodyBuilder.MultipartBody multipart = binaryExportService.buildMultipartByIds(records);
            return toMultipartResponse(multipart);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to build package");
        }
    }

    private static Instant parseSince(String s) {
        if (!s.isEmpty() && s.chars().allMatch(Character::isDigit)) {
            return Instant.ofEpochMilli(Long.parseLong(s));
        }
        return Instant.parse(s);
    }

    private static ResponseEntity<byte[]> toMultipartResponse(MultipartMixedBodyBuilder.MultipartBody multipart) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("multipart/mixed; boundary=" + multipart.boundary()));
        return new ResponseEntity<>(multipart.body(), headers, HttpStatus.OK);
    }
}
