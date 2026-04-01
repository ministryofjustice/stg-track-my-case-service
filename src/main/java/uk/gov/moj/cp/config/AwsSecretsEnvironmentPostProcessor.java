package uk.gov.moj.cp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AwsSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsEnvironmentPostProcessor.class);
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

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String secretName = environment.getProperty(SECRET_NAME_ENV);
        if (secretName == null || secretName.isBlank()) {
            secretName = environment.getProperty(SECRET_NAME_PROPERTY);
        }
        if (secretName == null || secretName.isBlank()) {
            String msg = "AWS Secrets Manager: TMC_AWS_SECRET_NAME not set; TMC* keys will not be loaded from AWS";
            log.info(msg);
            return;
        }

        log.info("Loading TMC* secrets from AWS Secrets Manager: secretName={}", secretName);

        String region = environment.getProperty(REGION_ENV);
        if (region == null || region.isBlank()) {
            region = environment.getProperty(AWS_REGION_ENV);
        }
        if (region == null || region.isBlank()) {
            region = environment.getProperty(REGION_PROPERTY);
        }
        if (region == null || region.isBlank()) {
            region = DEFAULT_REGION;
        }
        Map<String, String> secrets = null;

        try {
            secrets = AwsSecretsLoader.loadSecret(secretName, region);
        } catch (Exception e) {
            String errorMsg = "Error loading secrets from AWS Secrets Manager for secretName=" + secretName + ": " + e.getMessage();
            log.error(errorMsg, e);
            return;
        }

        if (secrets.isEmpty()) {
            String warnMsg = "AWS Secrets Manager: No secrets loaded for secretName=" + secretName;
            log.warn("No secrets loaded from AWS Secrets Manager for secretName={}", secretName);
            return;
        }
        String str = secrets.keySet().stream().collect(Collectors.joining(", "));
        String keysMsg = "AWS Secrets Manager: Keys loaded from secret " + secretName + ": " + str;
        log.info("Keys loaded from AWS Secrets Manager secret {}: {}", secretName, str);

        Map<String, Object> props = new HashMap<>();
        putAllTmcPrefixed(secrets, props);

        if (!props.isEmpty()) {
            // Highest precedence so AWS-secret values override same-named env vars from deploy tooling.
            environment.getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
            String populated = "AWS Secrets Manager: Populated " + props.size() + " TMC* keys from AWS";
            System.out.println(populated);
            for (String key : props.keySet()) {
                log.info("TMC config populated from AWS Secrets Manager: {} (value length={})",
                    key, ((String) props.get(key)).length());
            }
        } else {
            log.warn("AWS secret contained no TMC-prefixed keys; keys in secret: {}",
                secrets.keySet());
        }
    }

    private static void putAllTmcPrefixed(Map<String, String> secrets, Map<String, Object> target) {
        for (Map.Entry<String, String> entry : secrets.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && key.startsWith(TMC_KEY_PREFIX) && value != null && !value.isBlank()) {
                target.put(key, value);
            }
        }
    }
}
