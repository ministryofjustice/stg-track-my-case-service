package uk.gov.moj.cp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class SecretKeyLoader {

    @Getter
    private final SecretKeySpec aesSecretKeySpec;

    @Getter
    private final byte[] hmacEncryptionSecret;

    public SecretKeyLoader(@Value("${services.users.aes-encryption-secret}") String aesEncryptionSecret,
                           @Value("${services.users.hmac-encryption-secret}") String hmacEncryptionSecret) {
        byte[] key = Base64.getDecoder().decode(aesEncryptionSecret);
        if (key.length != 32) {
            throw new IllegalStateException("ENC_KEY_B64 must decode to 32 bytes for AES-256");
        }
        this.aesSecretKeySpec = new SecretKeySpec(key, "AES");
        this.hmacEncryptionSecret = hmacEncryptionSecret.getBytes();
    }
}
