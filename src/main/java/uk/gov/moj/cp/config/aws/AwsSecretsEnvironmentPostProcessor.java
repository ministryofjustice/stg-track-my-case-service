package uk.gov.moj.cp.config.aws;

import org.apache.commons.logging.Log;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.stream.Collectors;

public class AwsSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "awsSecretsManager";
    private static final String SECRET_NAME_ENV = "TMC_AWS_SECRET_NAME";
    private static final String SECRET_NAME_PROPERTY = "tmc.aws.secret-name";
    private static final String TMC_REGION_ENV = "TMC_AWS_REGION";
    /** Fallback when no env/config set (e.g. match vars.DEV_ECR_REGION for UK). */
    private static final String DEFAULT_REGION = "eu-west-2";
    private static final String TMC_KEY_PREFIX = "TMC";

    private final Log log;

    public AwsSecretsEnvironmentPostProcessor(final DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(AwsSecretsEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        String secretName = environment.getProperty(SECRET_NAME_ENV);
        if (Strings.isEmpty(secretName)) {
            secretName = environment.getProperty(SECRET_NAME_PROPERTY);
        }
        if (Strings.isEmpty(secretName)) {
            log.info("AWS Secrets Manager: TMC_AWS_SECRET_NAME not set; TMC* keys will not be loaded from AWS");
            return;
        }

        String region = environment.getProperty(TMC_REGION_ENV);
        if (Strings.isEmpty(region)) {
            region = DEFAULT_REGION;
        }
        try {
            Map<String, String> secrets = AwsSecretsLoader.loadSecret(log, secretName, region);
            if (secrets.isEmpty()) {
                log.warn("No secrets loaded from AWS Secrets Manager for secretName=" + secretName);
                return;
            }
            final Map<String, Object> tmcSecrets =  getAllTMCSecrets(secrets);
            if (!tmcSecrets.isEmpty()) {
                // Highest precedence so AWS-secret values override same-named env vars from deploy tooling.
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, tmcSecrets));
                log.info("AWS Secrets Manager: Populated ; Number of TMC* secrets :" + tmcSecrets.size());
                tmcSecrets.keySet().forEach(key -> log.info(key + ": value length=" + tmcSecrets.get(key).toString().length()+";"));
            } else {
                log.warn("AWS secret contained no TMC-prefixed keys; keys in secret: " + secrets.keySet());
            }
        } catch (Exception exception) {
            log.error("Error loading secrets from AWS Secrets Manager for secretName=" + secretName + ": " + exception);
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
