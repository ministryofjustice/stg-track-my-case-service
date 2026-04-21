package uk.gov.moj.cp.config.aws;

import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring Boot does not call {@link org.springframework.boot.logging.DeferredLog#replayTo} for custom
 * {@link org.springframework.boot.env.EnvironmentPostProcessor} beans, so buffered lines never reach
 * Logback. Replay once the logging system is up, before the context is refreshed (so messages appear
 * before Flyway and any later startup failure).
 */
public class AwsSecretsDeferredLogReplayListener implements SpringApplicationRunListener {

    public AwsSecretsDeferredLogReplayListener(final SpringApplication application, final String[] args) {
        // Required for Spring Boot SPI
    }

    @Override
    public void contextLoaded(final ConfigurableApplicationContext context) {
        AwsSecretsEnvironmentPostProcessor.replayDeferredLogTo(
            LogFactory.getLog(AwsSecretsEnvironmentPostProcessor.class));
    }
}
