package uk.gov.moj.cp.config.aws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Collections;
import java.util.Map;

@Slf4j
public final class AwsSecretsLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private AwsSecretsLoader() {
    }

    public static Map<String, String> loadSecret(final String secretName, final String region) {
        log.info("Attempting to load secret from AWS Secrets Manager: secretName={}, region={}", secretName, region);

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
                System.out.println("AwsSecretsLoader: Secret string empty for secretName=" + secretName);
                return Collections.emptyMap();
            }

            Map<String, String> parsedSecrets = OBJECT_MAPPER.readValue(
                secretString,
                new TypeReference<>() {
                }
            );

            String successMsg = "AwsSecretsLoader: Loaded " + parsedSecrets.size() + " keys from AWS Secrets Manager secret " + secretName + " (keys: " + parsedSecrets.keySet() + ")";
            System.out.println(successMsg);
            log.info("Loaded {} keys from AWS Secrets Manager secret: {} (keys: {})",
                parsedSecrets.size(), secretName, parsedSecrets.keySet());
            return parsedSecrets;
        } catch (Exception e) {
            log.error("Failed to load secret from AWS Secrets Manager: secretName={}", secretName, e);
            return Collections.emptyMap();
        }
    }
}
