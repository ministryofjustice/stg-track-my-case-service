package uk.gov.moj.cp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cp.config.SecretKeyLoader;

import javax.crypto.spec.SecretKeySpec;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cp.repository.TestCryptoConfig.generateBase64Key;

public class CryptoUtilsTest {

    private static final String HMAC_ENCRYPTION_SECRET = generateBase64Key();
    private final SecretKeyLoader secretKeyLoader = new SecretKeyLoader(
        generateBase64Key(), HMAC_ENCRYPTION_SECRET
    );
    private final SecretKeyLoader anotherSecretKeyLoader = new SecretKeyLoader(
        generateBase64Key(), HMAC_ENCRYPTION_SECRET);

    @Test
    @DisplayName("Should return null or initial value")
    void testReturnNull() {
        assertThat(CryptoUtils.aesEncrypt(null, secretKeyLoader.getAesSecretKeySpec())).isEqualTo(null);

        assertThat(CryptoUtils.aesDecrypt(null, secretKeyLoader.getAesSecretKeySpec())).isEqualTo(null);
        assertThat(CryptoUtils.aesDecrypt("bad-prefix:xxxx", secretKeyLoader.getAesSecretKeySpec()))
            .isEqualTo("bad-prefix:xxxx");
    }

    @Test
    @DisplayName("Should encrypt and decrypt value")
    void testAesEncryptAesDecrypt() {
        final String initialValue = "my@email.com";
        String encrypted = CryptoUtils.aesEncrypt(initialValue, secretKeyLoader.getAesSecretKeySpec());
        assertThat(encrypted).startsWith(CryptoUtils.ENCRYPTION_PREFIX);

        final String decrypted = CryptoUtils.aesDecrypt(encrypted, secretKeyLoader.getAesSecretKeySpec());
        assertThat(initialValue).isEqualTo(decrypted);
    }

    @Test
    @DisplayName("Should keep same encrypted value")
    void testAesEncryptSameValue() {
        final String initialValue = "my@email.com";
        String encrypted1 = CryptoUtils.aesEncrypt(initialValue, secretKeyLoader.getAesSecretKeySpec());
        String encrypted2 = CryptoUtils.aesEncrypt(initialValue, secretKeyLoader.getAesSecretKeySpec());
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should encrypt and fail on decryption with other secret key")
    void testAesEncryptAesDecryptFailedOnOtherSecretKey() {
        final String initialValue = "my@email.com";
        String encrypted = CryptoUtils.aesEncrypt(initialValue, secretKeyLoader.getAesSecretKeySpec());
        assertThat(encrypted).startsWith(CryptoUtils.ENCRYPTION_PREFIX);

        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> {
                CryptoUtils.aesDecrypt(encrypted, anotherSecretKeyLoader.getAesSecretKeySpec());
            }
        );
        assertThat(illegalStateException.getMessage()).isEqualTo("Attribute decryption failed");
    }

    @Test
    @DisplayName("Should failed on encryption with random 18 bytes")
    void testFailOnEncryption() {
        final String initialValue = "my@email.com";
        byte[] secret = new byte[18];
        new SecureRandom().nextBytes(secret);
        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> {
                CryptoUtils.aesEncrypt(initialValue, new SecretKeySpec(secret, "AES"));
            }
        );
        assertThat(illegalStateException.getMessage()).isEqualTo("Invalid AES key length: 18 bytes");
    }

    @Test
    @DisplayName("Should fail on encryption with bad secret key format")
    void testFailOnEncryptionWithBadSecretKey() {
        final String initialValue = "my@email.com";
        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> {
                CryptoUtils.aesEncrypt(initialValue, new SecretKeySpec("this-is-not-base64".getBytes(), "AES"));
            }
        );
        assertThat(illegalStateException.getMessage()).isEqualTo("Invalid AES key length: 18 bytes");
    }

    @Test
    @DisplayName("Should keep same encrypted value")
    void testHmacEncryptSameValue() {
        final String initialValue = "my@email.com";
        String encrypted1 = CryptoUtils.hmacEncrypt(initialValue, secretKeyLoader.getHmacEncryptionSecret());
        String encrypted2 = CryptoUtils.hmacEncrypt(initialValue, secretKeyLoader.getHmacEncryptionSecret());
        assertThat(encrypted1).isEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should fail on encryption with bad secret key format")
    void testHmacFailure() {
        final String initialValue = "my@email.com";
        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> {
                CryptoUtils.hmacEncrypt(initialValue, "".getBytes());
            }
        );
        assertThat(illegalStateException.getMessage()).isEqualTo("Attribute decryption failed");
    }


}
