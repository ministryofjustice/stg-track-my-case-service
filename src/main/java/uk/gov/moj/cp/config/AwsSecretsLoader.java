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

/**
 * Loads secrets from AWS Secrets Manager.
 * Expects the secret to be stored as JSON with keys such as TMC_DB_URL, TMC_TOKEN_CLIENT_ID.
 * Used by {@link AwsSecretsEnvironmentPostProcessor} before the Spring context starts.
 */
public final class AwsSecretsLoader {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsLoader.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AwsSecretsLoader() {
    }

    /**
     * Fetches the secret from AWS Secrets Manager and returns its key-value pairs.
     *
     * @param secretName the secret name or ARN (e.g. tmc/prod or arn:aws:secretsmanager:...)
     * @param region     AWS region (e.g. eu-west-2); if null, uses default region from SDK
     * @return map of secret keys to values, or empty map on error
     */
    public static Map<String, String> loadSecret(String secretName, String region) {
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
                return Collections.emptyMap();
            }

            Map<String, String> parsed = OBJECT_MAPPER.readValue(
                secretString,
                new TypeReference<>() {
                }
            );
            log.info("Loaded {} keys from AWS Secrets Manager secret: {} (keys: {})",
                parsed.size(), secretName, parsed.keySet());
            return parsed;
        } catch (Exception e) {
            log.error("Failed to load secret from AWS Secrets Manager: secretName={}", secretName, e);
            return Collections.emptyMap();
        }
    }
}
