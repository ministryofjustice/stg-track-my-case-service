package uk.gov.moj.cp.config.aws;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import uk.gov.moj.cp.util.Utils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;

public class AwsSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String TMC_AWS_ENABLED = "tmc.aws.enabled";
    private static final String TMC_AWS_SECRET_NAME = "tmc.aws.secret-name";
    private static final String TMC_AWS_REGION = "tmc.aws.region";
    private static final String PROPERTY_SOURCE_NAME = "awsSecretsManager";
    private static final String TMC_KEY_PREFIX = "TMC";

    private final Log log;

    public AwsSecretsEnvironmentPostProcessor(final DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(AwsSecretsEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        if (!Boolean.parseBoolean(Optional.ofNullable(environment.getProperty(TMC_AWS_ENABLED)).orElse("false"))) {
            log.info("AWS Secrets Manager: not enabled");
            return;
        }

        final String secretName = environment.getProperty(TMC_AWS_SECRET_NAME);
        if (Strings.isEmpty(secretName)) {
            log.info(format(
                "AWS Secrets Manager: %s not set; TMC* keys will not be loaded from AWS", TMC_AWS_SECRET_NAME));
            return;
        }

        final String region = environment.getProperty(TMC_AWS_REGION);
        if (Strings.isEmpty(region)) {
            log.info(format(
                "AWS Secrets Manager: %s not set; TMC* keys will not be loaded from AWS", TMC_AWS_REGION));
            return;
        }
        try {
            Map<String, String> secrets = loadSecret(secretName, region);
            if (secrets.isEmpty()) {
                log.warn(format("No secrets loaded from AWS Secrets Manager for secretName=%s", secretName));
                return;
            }
            final Map<String, Object> tmcSecrets =  getAllTMCSecrets(secrets);
            if (!tmcSecrets.isEmpty()) {
                // Highest precedence so AWS-secret values override same-named env vars from deploy tooling.
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, tmcSecrets));
                log.info(format(
                    "AWS Secrets Manager: Populated ; Number of TMC* secrets :%d", tmcSecrets.size()));
                tmcSecrets.keySet().forEach(key -> log.info(format(
                    "%s: value length=%d;", key, tmcSecrets.get(key).toString().length())));
            } else {
                log.warn(format(
                    "AWS secret contained no TMC-prefixed keys; keys in secret: %s", join(",", secrets.keySet())));
            }
        } catch (Exception exception) {
            log.error(format(
                "Error loading secrets from AWS Secrets Manager for secretName=%s: %s", secretName, exception));
        }
    }

    /**
     * Loads and parses a JSON secret from AWS Secrets Manager
     */
    protected Map<String, String> loadSecret(final String secretName, final String region) {
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
                log.warn(format("AWS Secrets: Secret string empty for secretName=%s", secretName));
                return Collections.emptyMap();
            }

            Map<String, String> parsedSecrets = Utils.objectMapper.readValue(
                secretString,
                new TypeReference<>() {
                }
            );
            log.info(format(
                "AWS Secrets: Loaded %s keys from AWS Secrets Manager secret name [%s],  keys: [%s]",
                parsedSecrets.size(), secretName, join(",", parsedSecrets.keySet())));
            return parsedSecrets;
        } catch (Exception e) {
            log.error(
                format("AWS Secrets: Failed to load secret from AWS Secrets Manager: secretName=%s", secretName),
                e);
            return Collections.emptyMap();
        }
    }

    private static Map<String, Object> getAllTMCSecrets(final Map<String, String> secrets) {
        return secrets.entrySet().stream()
                .filter(entry -> !Strings.isEmpty(entry.getKey())
                        && entry.getKey().startsWith(TMC_KEY_PREFIX)
                        && !Strings.isEmpty(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
