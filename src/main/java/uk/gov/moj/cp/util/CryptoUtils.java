package uk.gov.moj.cp.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {

    public static final String ENCRYPTION_PREFIX = "ENC1:";   // version marker

    private static final int IV_LEN_BYTES = 12;     // 96 bits
    private static final int TAG_LEN_BITS = 128;    // per NIST SP 800-38D
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGO = "HmacSHA256";

    private static final SecureRandom RNG = new SecureRandom();

    public static String aesEncrypt(String plaintext, Key secretKey) throws IllegalStateException {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            Base64.Encoder encoder = Base64.getEncoder();
            return ENCRYPTION_PREFIX + encoder.encodeToString(iv) + ":" + encoder.encodeToString(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static String aesDecrypt(String dbData, Key secretKey) throws IllegalStateException {
        if (dbData == null) {
            return null;
        }
        if (!dbData.startsWith(ENCRYPTION_PREFIX)) {
            return dbData;
        }
        try {
            final String[] parts = dbData.substring(ENCRYPTION_PREFIX.length()).split(":", 2);
            final byte[] iv = Base64.getDecoder().decode(parts[0]);
            final byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] plain = cipher.doFinal(ciphertext);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Attribute decryption failed", e);
        }
    }

    public static String hmacEncrypt(final String plaintext, final byte[] secretLookupKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretLookupKey, HMAC_ALGO);
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(keySpec);
            byte[] h = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException("Attribute decryption failed", e);
        }
    }

}
