package uk.gov.moj.cp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads TMC_DB_URL and TMC_TOKEN_CLIENT_ID from AWS Secrets Manager before the application context
 * starts, so they are available for {@code ${TMC_DB_URL}} and {@code ${TMC_TOKEN_CLIENT_ID}} in
 * application.yaml.
 * <p>
 * Only runs when {@code TMC_AWS_SECRET_NAME} is set (e.g. in Kubernetes). The secret in AWS
 * should be a JSON object with keys {@code TMC_DB_URL} and {@code TMC_TOKEN_CLIENT_ID}.
 * Region can be set via {@code AWS_REGION} or {@code TMC_AWS_REGION}.
 */
public class AwsSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "awsSecretsManager";
    private static final String SECRET_NAME_ENV = "TMC_AWS_SECRET_NAME";
    private static final String REGION_ENV = "TMC_AWS_REGION";
    private static final String AWS_REGION_ENV = "AWS_REGION";

    private static final String TMC_DB_URL = "TMC_DB_URL";
    private static final String TMC_TOKEN_CLIENT_ID = "TMC_TOKEN_CLIENT_ID";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Trace so we know the processor ran (visible even if logging is not yet configured)
        System.err.println("[AwsSecretsEnvironmentPostProcessor] postProcessEnvironment called (runs before config/beans)");

        String secretName = environment.getProperty(SECRET_NAME_ENV);
        if (secretName == null || secretName.isBlank()) {
            log.info("AWS Secrets Manager: TMC_AWS_SECRET_NAME not set; TMC_DB_URL and TMC_TOKEN_CLIENT_ID will not be loaded from AWS");
            return;
        }

        log.info("Loading TMC_DB_URL and TMC_TOKEN_CLIENT_ID from AWS Secrets Manager: secretName={}", secretName);

        String region = environment.getProperty(REGION_ENV);
        if (region == null || region.isBlank()) {
            region = environment.getProperty(AWS_REGION_ENV);
        }

        Map<String, String> secrets = AwsSecretsLoader.loadSecret(secretName, region);
        if (secrets.isEmpty()) {
            log.warn("No secrets loaded from AWS Secrets Manager for secretName={}", secretName);
            return;
        }

        Map<String, Object> props = new HashMap<>();
        putIfPresent(secrets, props, TMC_DB_URL);
        putIfPresent(secrets, props, TMC_TOKEN_CLIENT_ID);

        if (!props.isEmpty()) {
            environment.getPropertySources()
                .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
            for (String key : props.keySet()) {
                log.info("TMC config populated from AWS Secrets Manager: {} (value length={})",
                    key, ((String) props.get(key)).length());
            }
        } else {
            log.warn("AWS secret did not contain TMC_DB_URL or TMC_TOKEN_CLIENT_ID; keys in secret: {}",
                secrets.keySet());
        }
    }

    private static void putIfPresent(Map<String, String> secrets, Map<String, Object> target, String key) {
        String value = secrets.get(key);
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
