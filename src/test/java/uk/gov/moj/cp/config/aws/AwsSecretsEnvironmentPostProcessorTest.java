package uk.gov.moj.cp.config.aws;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AwsSecretsEnvironmentPostProcessor} tests with {@link AwsSecretsLoader} stubbed via {@link MockedStatic}.
 */
class AwsSecretsEnvironmentPostProcessorTest {

    @Test
    void postProcessEnvironment_addsPropertySource_whenLoadSecretReturnsTmcKeys() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("my-secret"), eq("eu-west-2")))
                .thenReturn(Map.of("TMC_DB_URL", "jdbc:postgresql://mock/db"));

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(
                new MapPropertySource("testProps", Map.of("TMC_AWS_SECRET_NAME", "my-secret"))
            );

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getProperty("TMC_DB_URL")).isEqualTo("jdbc:postgresql://mock/db");
            assertThat(env.getPropertySources().get("awsSecretsManager"))
                .isInstanceOf(MapPropertySource.class);

            aws.verify(() -> AwsSecretsLoader.loadSecret(eq(log), eq("my-secret"), eq("eu-west-2")));
        }
    }

    @Test
    void postProcessEnvironment_addsPropertySource_whenSecretNameOnlyFromYamlProperty() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("yaml-secret-id"), eq("eu-west-2")))
                .thenReturn(Map.of("TMC_APP", "from-yaml-name"));

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(
                new MapPropertySource("testProps", Map.of("tmc.aws.secret-name", "yaml-secret-id"))
            );

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getProperty("TMC_APP")).isEqualTo("from-yaml-name");
            aws.verify(() -> AwsSecretsLoader.loadSecret(eq(log), eq("yaml-secret-id"), eq("eu-west-2")));
        }
    }

    @Test
    void postProcessEnvironment_prefersEnvSecretNameOverYamlPropertyWhenBothPresent() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            Map<String, Object> props = new HashMap<>();
            props.put("TMC_AWS_SECRET_NAME", "env-secret");
            props.put("tmc.aws.secret-name", "yaml-secret");
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("env-secret"), eq("eu-west-2")))
                .thenReturn(Map.of("TMC_ONE", "1"));

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("testProps", props));

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getProperty("TMC_ONE")).isEqualTo("1");
            aws.verify(() -> AwsSecretsLoader.loadSecret(eq(log), eq("env-secret"), eq("eu-west-2")));
            aws.verify(() -> AwsSecretsLoader.loadSecret(eq(log), eq("yaml-secret"), eq("eu-west-2")), times(0));
        }
    }

    @Test
    void postProcessEnvironment_resolvesRegionFromTmcAwsRegion() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("s"), eq("eu-west-1")))
                .thenReturn(Map.of("TMC_REG", "ok"));

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(
                new MapPropertySource("testProps",
                                      Map.of(
                                          "TMC_AWS_SECRET_NAME", "s",
                                          "TMC_AWS_REGION", "eu-west-1"))
            );

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getProperty("TMC_REG")).isEqualTo("ok");
            aws.verify(() -> AwsSecretsLoader.loadSecret(eq(log), eq("s"), eq("eu-west-1")));
        }
    }


    @Test
    void postProcessEnvironment_doesNotAddAwsSource_whenLoadSecretReturnsEmptyMap() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("my-secret"), eq("eu-west-2")))
                .thenReturn(Map.of());

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(
                new MapPropertySource("testProps", Map.of("TMC_AWS_SECRET_NAME", "my-secret"))
            );

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getPropertySources().get("awsSecretsManager")).isNull();
        }
    }

    @Test
    void postProcessEnvironment_doesNotAddAwsSource_whenOnlyNonTmcKeysInSecret() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("my-secret"), eq("eu-west-2")))
                .thenReturn(Map.of("OTHER_KEY", "value"));

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(
                new MapPropertySource("testProps", Map.of("TMC_AWS_SECRET_NAME", "my-secret"))
            );

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getPropertySources().get("awsSecretsManager")).isNull();
            verify(log).warn(argThat(msg -> msg != null && msg.toString().contains("no TMC-prefixed keys")));
        }
    }

    @Test
    void postProcessEnvironment_mapsMultipleTmcKeysFromSecret() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("my-secret"), eq("eu-west-2")))
                .thenReturn(Map.of(
                    "TMC_DB_URL", "jdbc:a",
                    "TMC_TOKEN", "token-value"));

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(
                new MapPropertySource("testProps", Map.of("TMC_AWS_SECRET_NAME", "my-secret"))
            );

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getProperty("TMC_DB_URL")).isEqualTo("jdbc:a");
            assertThat(env.getProperty("TMC_TOKEN")).isEqualTo("token-value");
            MapPropertySource awsSource = (MapPropertySource) env.getPropertySources().get("awsSecretsManager");
            assertThat(awsSource.getProperty("TMC_DB_URL")).isEqualTo("jdbc:a");
            assertThat(awsSource.getProperty("TMC_TOKEN")).isEqualTo("token-value");
        }
    }

    @Test
    void postProcessEnvironment_tmcSecretOverridesExistingPropertyWithSameKey() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log log = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);

        try (MockedStatic<AwsSecretsLoader> aws = mockStatic(AwsSecretsLoader.class)) {
            aws.when(() -> AwsSecretsLoader.loadSecret(eq(log), eq("my-secret"), eq("eu-west-2")))
                .thenReturn(Map.of("TMC_DB_URL", "jdbc:new"));

            AwsSecretsEnvironmentPostProcessor processor = new AwsSecretsEnvironmentPostProcessor(logFactory);
            StandardEnvironment env = new StandardEnvironment();
            Map<String, Object> props = new HashMap<>();
            props.put("TMC_AWS_SECRET_NAME", "my-secret");
            props.put("TMC_DB_URL", "jdbc:old");
            env.getPropertySources().addFirst(new MapPropertySource("testProps", props));

            processor.postProcessEnvironment(env, mock(SpringApplication.class));

            assertThat(env.getProperty("TMC_DB_URL")).isEqualTo("jdbc:new");
        }
    }
}
