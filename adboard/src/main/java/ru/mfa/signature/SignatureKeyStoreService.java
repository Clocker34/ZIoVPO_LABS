package ru.mfa.signature;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Service
public class SignatureKeyStoreService {

    private final SignatureProperties properties;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Certificate certificate;

    public SignatureKeyStoreService(SignatureProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws Exception {
        if (properties.getKeyStorePath() == null || properties.getKeyStorePath().isBlank()) {
            throw new IllegalStateException("Signature keystore path is not configured.");
        }

        KeyStore keyStore = KeyStore.getInstance(properties.getKeyStoreType() != null ? properties.getKeyStoreType() : KeyStore.getDefaultType());
        
        URL url = ResourceUtils.getURL(properties.getKeyStorePath());
        char[] storePass = properties.getKeyStorePassword() != null ? properties.getKeyStorePassword().toCharArray() : null;
        
        try (InputStream is = url.openStream()) {
            keyStore.load(is, storePass);
        }

        String alias = properties.getKeyAlias();
        if (!keyStore.containsAlias(alias)) {
            throw new IllegalArgumentException("Keystore does not contain alias: " + alias);
        }

        char[] keyPass = properties.getKeyPassword() != null ? properties.getKeyPassword().toCharArray() : storePass;
        this.privateKey = (PrivateKey) keyStore.getKey(alias, keyPass);
        this.certificate = keyStore.getCertificate(alias);
        if (this.certificate != null) {
            this.publicKey = this.certificate.getPublicKey();
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public Certificate getCertificate() {
        return certificate;
    }
}
