package ru.rkjrth.adboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.rkjrth.adboard.dto.CreateSignatureRequest;
import ru.rkjrth.adboard.entity.MalwareSignature;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.UserRepository;
import ru.rkjrth.adboard.service.MalwareSignatureService;
import ru.rkjrth.adboard.service.SignatureBinaryExportService;
import ru.mfa.signature.SigningService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BinarySignatureExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MalwareSignatureService malwareSignatureService;

    @Autowired
    private SignatureBinaryExportService binaryExportService;

    @Autowired
    private SigningService signingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;

    @BeforeEach
    void seed() {
        admin = new User();
        admin.setUsername("admin_bin");
        admin.setEmail("admin_bin@t.test");
        admin.setName("Admin");
        admin.setPasswordHash(passwordEncoder.encode("Secret1!"));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);
    }

    @Test
    void fullExport_manifestSignatureAndDataShaMatch() throws Exception {
        MalwareSignature sig = malwareSignatureService.create(sampleCreate(), admin.getUsername());

        SignatureBinaryExportService.BinaryParts parts = binaryExportService.buildFull(List.of(sig));

        byte[] dataBin = parts.dataBin();
        byte[] expectedSha = MessageDigest.getInstance("SHA-256").digest(dataBin);

        byte[] manifest = parts.manifestBin();
        byte[] unsigned = extractUnsignedManifest(manifest);
        byte[] manifestSig = extractManifestSignature(manifest);

        assertThat(signingService.verifyBytes(unsigned, manifestSig)).isTrue();

        ByteBuffer buf = ByteBuffer.wrap(unsigned);
        buf.position(skipMagicAndHeader(buf, 0));
        byte[] shaFromManifest = new byte[32];
        buf.get(shaFromManifest);
        assertThat(shaFromManifest).isEqualTo(expectedSha);

        assertThat(readExportType(unsigned)).isEqualTo(0);
        assertThat(readSinceMillis(unsigned)).isEqualTo(-1L);
    }

    @Test
    void fullExport_excludesDeleted() throws Exception {
        MalwareSignature a = malwareSignatureService.create(sampleCreate(), admin.getUsername());
        CreateSignatureRequest bReq = sampleCreate();
        bReq.setThreatName("Other");
        MalwareSignature b = malwareSignatureService.create(bReq, admin.getUsername());
        malwareSignatureService.delete(b.getId(), admin.getUsername());

        SignatureBinaryExportService.BinaryParts parts = binaryExportService.buildFull(
                malwareSignatureService.getAllActual());

        assertThat(readRecordCount(parts.manifestBin())).isEqualTo(1);
        assertThat(readFirstEntryId(parts.manifestBin())).isEqualTo(a.getId());
    }

    @Test
    void incrementIncludesDeleted() throws Exception {
        MalwareSignature a = malwareSignatureService.create(sampleCreate(), admin.getUsername());
        malwareSignatureService.delete(a.getId(), admin.getUsername());

        Instant since = Instant.EPOCH;
        SignatureBinaryExportService.BinaryParts parts = binaryExportService.buildIncrement(
                malwareSignatureService.getIncrement(since), since.toEpochMilli());

        assertThat(readRecordCount(parts.manifestBin())).isEqualTo(1);
        assertThat(readFirstEntryStatus(unsignedManifest(parts.manifestBin()))).isEqualTo(2);
        assertThat(readExportType(unsignedManifest(parts.manifestBin()))).isEqualTo(1);
    }

    @Test
    void binaryFull_httpMultipart() throws Exception {
        malwareSignatureService.create(sampleCreate(), admin.getUsername());

        MvcResult result = mockMvc.perform(get("/api/binary/signatures/full")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String ct = result.getResponse().getContentType();
        assertThat(ct).contains("multipart/mixed");
        assertThat(ct).contains("boundary=");
        assertThat(result.getResponse().getContentAsByteArray().length).isPositive();
    }

    @Test
    void binaryIncrement_badSince_returns400() throws Exception {
        mockMvc.perform(get("/api/binary/signatures/increment")
                        .param("since", "not-a-date")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void binaryByIds_returnsMultipart() throws Exception {
        MalwareSignature sig = malwareSignatureService.create(sampleCreate(), admin.getUsername());

        MvcResult result = mockMvc.perform(post("/api/binary/signatures/by-ids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + sig.getId() + "\"]}")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("multipart/mixed");
    }

    private static CreateSignatureRequest sampleCreate() {
        CreateSignatureRequest create = new CreateSignatureRequest();
        create.setThreatName("Trojan.Win32.Bin");
        create.setFirstBytesHex("4D5A9000");
        create.setRemainderHashHex("A1B2C3D4");
        create.setRemainderLength(512);
        create.setFileType("EXE");
        create.setOffsetStart(0);
        create.setOffsetEnd(512);
        return create;
    }

    /**
     * Trailer layout: {@code [unsigned...][uint32 manifestSigLen][manifestSigLen bytes]} — length precedes signature.
     */
    private static byte[] extractUnsignedManifest(byte[] manifest) {
        return splitManifest(manifest)[0];
    }

    private static byte[] extractManifestSignature(byte[] manifest) {
        return splitManifest(manifest)[1];
    }

    private static byte[][] splitManifest(byte[] manifest) {
        if (manifest.length < 4 + 64) {
            throw new IllegalArgumentException("manifest too short");
        }
        for (int sigLen = manifest.length - 4; sigLen >= 64; sigLen--) {
            int lenPos = manifest.length - sigLen - 4;
            if (lenPos < 0) {
                continue;
            }
            int declared = ByteBuffer.wrap(manifest, lenPos, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            if (declared == sigLen) {
                byte[] unsigned = Arrays.copyOfRange(manifest, 0, lenPos);
                byte[] sig = Arrays.copyOfRange(manifest, lenPos + 4, manifest.length);
                return new byte[][]{unsigned, sig};
            }
        }
        throw new IllegalArgumentException("cannot locate manifest signature trailer");
    }

    private static byte[] unsignedManifest(byte[] manifest) {
        return extractUnsignedManifest(manifest);
    }

    private static int skipMagicAndHeader(ByteBuffer buf, int start) {
        buf.position(start);
        int magicLen = buf.getShort() & 0xFFFF;
        buf.position(buf.position() + magicLen);
        // version u16, export u8, gen i64, since i64, count u32 = 2+1+8+8+4 = 23
        buf.position(buf.position() + 2 + 1 + 8 + 8 + 4);
        return buf.position();
    }

    private static int readRecordCount(byte[] manifest) {
        byte[] u = extractUnsignedManifest(manifest);
        ByteBuffer buf = ByteBuffer.wrap(u);
        int magicLen = buf.getShort() & 0xFFFF;
        buf.position(2 + magicLen + 2 + 1 + 8 + 8);
        return buf.getInt();
    }

    private static long readExportType(byte[] unsigned) {
        ByteBuffer buf = ByteBuffer.wrap(unsigned);
        int magicLen = buf.getShort() & 0xFFFF;
        buf.position(2 + magicLen + 2);
        return buf.get() & 0xFFL;
    }

    private static long readSinceMillis(byte[] unsigned) {
        ByteBuffer buf = ByteBuffer.wrap(unsigned);
        int magicLen = buf.getShort() & 0xFFFF;
        buf.position(2 + magicLen + 2 + 1 + 8);
        return buf.getLong();
    }

    private static UUID readFirstEntryId(byte[] manifest) {
        byte[] u = extractUnsignedManifest(manifest);
        ByteBuffer buf = ByteBuffer.wrap(u);
        int magicLen = buf.getShort() & 0xFFFF;
        buf.position(2 + magicLen + 2 + 1 + 8 + 8 + 4 + 32);
        long msb = buf.getLong();
        long lsb = buf.getLong();
        return new UUID(msb, lsb);
    }

    private static int readFirstEntryStatus(byte[] unsigned) {
        ByteBuffer buf = ByteBuffer.wrap(unsigned);
        int magicLen = buf.getShort() & 0xFFFF;
        buf.position(2 + magicLen + 2 + 1 + 8 + 8 + 4 + 32 + 16);
        return buf.get() & 0xFF;
    }
}

