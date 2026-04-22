package uk.gov.moj.cp.config.aws;

import org.apache.commons.logging.Log;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AwsSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "awsSecretsManager";
    private static final String SECRET_NAME_ENV = "TMC_AWS_SECRET_NAME";
    private static final String SECRET_NAME_PROPERTY = "tmc.aws.secret-name";
    private static final String REGION_ENV = "TMC_AWS_REGION";
    private static final String AWS_REGION_ENV = "AWS_REGION";
    private static final String REGION_PROPERTY = "tmc.aws.region";
    /** Fallback when no env/config set (e.g. match vars.DEV_ECR_REGION for UK). */
    private static final String DEFAULT_REGION = "eu-west-2";

    /** Secret JSON keys with this prefix are exposed as Spring properties. */
    private static final String TMC_KEY_PREFIX = "TMC";

    private final Log log;

    public AwsSecretsEnvironmentPostProcessor(final DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(AwsSecretsEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        try {
            doPostProcessEnvironment(environment);
        } finally {
            if (Strings.isEmpty(environment.getProperty("TMC_DB_URL"))) {
                log.warn("TMC_DB_URL is not set; spring.datasource.url will stay unresolved and Flyway will fail. "
                    + "Set TMC_DB_URL via env, Kubernetes secret, optional:configtree under /mnt/secrets/rpe/, "
                    + "or JSON key TMC_DB_URL in AWS Secrets Manager when TMC_AWS_SECRET_NAME or tmc.aws.secret-name is configured.");
            }
        }
    }

    private void doPostProcessEnvironment(final ConfigurableEnvironment environment) {
        String secretName = environment.getProperty(SECRET_NAME_ENV);
        if (Strings.isEmpty(secretName)) {
            secretName = environment.getProperty(SECRET_NAME_PROPERTY);
        }
        if (Strings.isEmpty(secretName)) {
            log.info("AWS Secrets Manager: TMC_AWS_SECRET_NAME not set; TMC* keys will not be loaded from AWS");
            return;
        }

        String region = environment.getProperty(REGION_ENV);
        if (Strings.isEmpty(region)) {
            region = environment.getProperty(AWS_REGION_ENV);
        }
        if (Strings.isEmpty(region)) {
            region = environment.getProperty(REGION_PROPERTY);
        }
        if (Strings.isEmpty(region)) {
            region = DEFAULT_REGION;
        }
        Map<String, String> secrets;

        try {
            secrets = AwsSecretsLoader.loadSecret(log, secretName, region);
        } catch (Exception e) {
            log.error("Error loading secrets from AWS Secrets Manager for secretName=" + secretName + ": " + e.getMessage(), e);
            return;
        }

        if (secrets.isEmpty()) {
            log.warn("No secrets loaded from AWS Secrets Manager for secretName=" + secretName);
            return;
        }
        final String keyNames = secrets.keySet().stream().collect(Collectors.joining(", "));
        log.info("Keys loaded from AWS Secrets Manager secret " + secretName + ": " + keyNames);

        final Map<String, Object> properties = new HashMap<>();
        getAllTMCSecrets(secrets, properties);

        if (!properties.isEmpty()) {
            // Highest precedence so AWS-secret values override same-named env vars from deploy tooling.
            environment.getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
            log.info("AWS Secrets Manager: Populated " + properties.size() + " TMC* keys from AWS");
            properties.keySet().forEach(key ->
                log.info("TMC config populated from AWS Secrets Manager: " + key + " (value length="
                    + ((String) properties.get(key)).length() + ")"));
        } else {
            log.warn("AWS secret contained no TMC-prefixed keys; keys in secret: " + secrets.keySet());
        }
    }

    private static void getAllTMCSecrets(final Map<String, String> secrets, final Map<String, Object> target) {
        for (Map.Entry<String, String> entry : secrets.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && key.startsWith(TMC_KEY_PREFIX) && value != null && !value.isBlank()) {
                target.put(key, value);
            }
        }
    }
}
