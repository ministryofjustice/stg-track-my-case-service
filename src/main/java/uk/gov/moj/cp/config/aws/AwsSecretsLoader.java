package uk.gov.moj.cp.config.aws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.util.Strings;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Collections;
import java.util.Map;

/**
 * Loads secrets from AWS; logging uses the caller's {@link org.springframework.boot.logging.DeferredLog}
 * so output is replayed after Logback starts.
 */
public final class AwsSecretsLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AwsSecretsLoader() {
    }

    public static Map<String, String> loadSecret(final Log log, final String secretName, final String region) {
        if (Strings.isEmpty(secretName)) {
            return Collections.emptyMap();
        }
        try (SecretsManagerClient client = !Strings.isEmpty(region)
            ? SecretsManagerClient.builder().region(Region.of(region)).build()
            : SecretsManagerClient.create()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();
            if (Strings.isEmpty(secretString)) {
                log.warn("AwsSecretsLoader: Secret string empty for secretName=" + secretName);
                return Collections.emptyMap();
            }

            Map<String, String> parsedSecrets = OBJECT_MAPPER.readValue(
                secretString,
                new TypeReference<>() {
                }
            );
            log.info("AwsSecretsLoader: Loaded " + parsedSecrets.size() + " keys from AWS Secrets Manager secret "
                + secretName + " (keys: " + parsedSecrets.keySet() + ")");
            return parsedSecrets;
        } catch (Exception e) {
            log.error("AwsSecretsLoader: Failed to load secret from AWS Secrets Manager: secretName=" + secretName, e);
            return Collections.emptyMap();
        }
    }
}
