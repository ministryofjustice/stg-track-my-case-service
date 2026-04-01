package uk.gov.moj.cp.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Collections;
import java.util.Map;

public final class AwsSecretsLoader {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsLoader.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AwsSecretsLoader() {
    }

    public static Map<String, String> loadSecret(String secretName, String region) {
        String attemptMsg = "AwsSecretsLoader: Attempting to load secret from AWS Secrets Manager: secretName=" + secretName + ", region=" + region;
        log.info("Attempting to load secret from AWS Secrets Manager: secretName={}, region={}", secretName, region);

        if (secretName == null || secretName.isBlank()) {
            return Collections.emptyMap();
        }
        try (SecretsManagerClient client = region != null && !region.isBlank()
            ? SecretsManagerClient.builder().region(Region.of(region)).build()
            : SecretsManagerClient.create()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();
            if (secretString == null || secretString.isBlank()) {
                System.out.println("AwsSecretsLoader: Secret string empty for secretName=" + secretName);
                return Collections.emptyMap();
            }

            Map<String, String> parsed = OBJECT_MAPPER.readValue(
                secretString,
                new TypeReference<>() {
                }
            );

            String successMsg = "AwsSecretsLoader: Loaded " + parsed.size() + " keys from AWS Secrets Manager secret " + secretName + " (keys: " + parsed.keySet() + ")";
            System.out.println(successMsg);
            log.info("Loaded {} keys from AWS Secrets Manager secret: {} (keys: {})",
                parsed.size(), secretName, parsed.keySet());
            return parsed;
        } catch (Exception e) {
            log.error("Failed to load secret from AWS Secrets Manager: secretName={}", secretName, e);
            return Collections.emptyMap();
        }
    }
}
