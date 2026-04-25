package ru.mfa.signature;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

@Service
public class SigningService {

    private final SignatureKeyStoreService keyStoreService;
    private final JsonCanonicalizer jsonCanonicalizer;

    public SigningService(SignatureKeyStoreService keyStoreService, JsonCanonicalizer jsonCanonicalizer) {
        this.keyStoreService = keyStoreService;
        this.jsonCanonicalizer = jsonCanonicalizer;
    }

    public String sign(Object payload) throws Exception {
        String canonicalJson = jsonCanonicalizer.canonizeJson(payload);
        byte[] payloadBytes = canonicalJson.getBytes(StandardCharsets.UTF_8);

        PrivateKey privateKey = keyStoreService.getPrivateKey();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payloadBytes);
        
        byte[] sigBytes = signature.sign();
        return Base64.getEncoder().encodeToString(sigBytes);
    }

    public boolean verify(Object payload, String signatureBase64) throws Exception {
        String canonicalJson = jsonCanonicalizer.canonizeJson(payload);
        byte[] payloadBytes = canonicalJson.getBytes(StandardCharsets.UTF_8);

        PublicKey publicKey = keyStoreService.getPublicKey();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(payloadBytes);
        
        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(sigBytes);
    }

    public String getPublicKeyDerBase64() {
        PublicKey publicKey = keyStoreService.getPublicKey();
        if (publicKey == null) return null;
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}
