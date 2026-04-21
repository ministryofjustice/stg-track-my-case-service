package uk.gov.moj.cp.config.aws;

import org.apache.commons.logging.Log;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Uses a single static {@link DeferredLog} so {@link AwsSecretsDeferredLogReplayListener} can replay
 * it after the logging system is initialised (see class javadoc there).
 */
public class AwsSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /** Shared with {@link AwsSecretsDeferredLogReplayListener#contextLoaded}. */
    private static final DeferredLog DEFERRED_LOG = new DeferredLog();

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

    static void replayDeferredLogTo(final Log target) {
        DEFERRED_LOG.replayTo(target);
    }

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        String secretName = environment.getProperty(SECRET_NAME_ENV);
        if (Strings.isEmpty(secretName)) {
            secretName = environment.getProperty(SECRET_NAME_PROPERTY);
        }
        if (Strings.isEmpty(secretName)) {
            DEFERRED_LOG.info("AWS Secrets Manager: TMC_AWS_SECRET_NAME not set; TMC* keys will not be loaded from AWS");
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
            secrets = AwsSecretsLoader.loadSecret(DEFERRED_LOG, secretName, region);
        } catch (Exception e) {
            DEFERRED_LOG.error("Error loading secrets from AWS Secrets Manager for secretName=" + secretName + ": " + e.getMessage(), e);
            return;
        }

        if (secrets.isEmpty()) {
            DEFERRED_LOG.warn("No secrets loaded from AWS Secrets Manager for secretName=" + secretName);
            return;
        }
        final String secretString = secrets.keySet().stream().collect(Collectors.joining(", "));
        DEFERRED_LOG.info("Keys loaded from AWS Secrets Manager secret " + secretName + ": " + secretString);

        final Map<String, Object> properties = new HashMap<>();
        getAllTMCSecrets(secrets, properties);

        if (!properties.isEmpty()) {
            // Highest precedence so AWS-secret values override same-named env vars from deploy tooling.
            environment.getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
            DEFERRED_LOG.info("AWS Secrets Manager: Populated " + properties.size() + " TMC* keys from AWS");
            properties.keySet().forEach(key ->
                DEFERRED_LOG.info("TMC config populated from AWS Secrets Manager: " + key + " (value length="
                    + ((String) properties.get(key)).length() + ")"));
        } else {
            DEFERRED_LOG.warn("AWS secret contained no TMC-prefixed keys; keys in secret: " + secrets.keySet());
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
