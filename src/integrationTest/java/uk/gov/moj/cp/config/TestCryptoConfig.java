package uk.gov.moj.cp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.security.SecureRandom;
import java.util.Base64;

@TestConfiguration
public class TestCryptoConfig {

    private static String generateBase64Key() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Bean
    public SecretKeyLoader secretKeyLoader() {
        return new SecretKeyLoader(
            generateBase64Key(),
            generateBase64Key()
        );
    }
}


