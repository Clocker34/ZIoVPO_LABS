package ru.rkjrth.adboard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.rkjrth.adboard.dto.license.Ticket;

import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * ЭЦП для {@link Ticket}: канонический JSON → SHA256withRSA → Base64.
 */
@Component
public class TicketSigner {

    private static final Logger log = LoggerFactory.getLogger(TicketSigner.class);

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @Value("${license.signature.private-key-base64:}")
    private String privateKeyBase64;

    @Value("${license.signature.public-key-base64:}")
    private String publicKeyBase64;

    private final ObjectMapper ticketMapper = new ObjectMapper();

    public TicketSigner() {
        ticketMapper.registerModule(new JavaTimeModule());
        ticketMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ticketMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @PostConstruct
    void initKeys() throws Exception {
        if (privateKeyBase64 != null && !privateKeyBase64.isBlank()) {
            byte[] pkcs8 = Base64.getDecoder().decode(privateKeyBase64.trim());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            if (publicKeyBase64 != null && !publicKeyBase64.isBlank()) {
                byte[] x509 = Base64.getDecoder().decode(publicKeyBase64.trim());
                publicKey = kf.generatePublic(new X509EncodedKeySpec(x509));
            } else {
                publicKey = derivePublicFromPrivate(privateKey);
            }
            return;
        }
        log.warn("license.signature.private-key-base64 not set — generating ephemeral RSA key pair for THIS RUN ONLY (dev)");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();
    }

    private static PublicKey derivePublicFromPrivate(PrivateKey privateKey)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        if (privateKey instanceof RSAPrivateCrtKey crt) {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }
        throw new IllegalArgumentException("RSA private key must be CRT form to derive public key");
    }

    public byte[] canonicalTicketJson(Ticket ticket) throws Exception {
        return ticketMapper.writeValueAsBytes(ticket);
    }

    public String signTicket(Ticket ticket) throws Exception {
        byte[] payload = canonicalTicketJson(ticket);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(payload);
        byte[] signature = sig.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    /** Проверка подписи (тесты, клиентские библиотеки на сервере — опционально). */
    public boolean verify(Ticket ticket, String signatureBase64) throws Exception {
        byte[] payload = canonicalTicketJson(ticket);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(payload);
        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        return sig.verify(sigBytes);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /** PEM SubjectPublicKeyInfo для выдачи клиентам проверки подписи. */
    public String publicKeyDerBase64() {
        byte[] encoded = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }
}
