package uk.gov.moj.cp.config.aws;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cp.config.aws.AwsSecretsEnvironmentPostProcessor.TMC_AWS_ENABLED;
import static uk.gov.moj.cp.config.aws.AwsSecretsEnvironmentPostProcessor.TMC_AWS_REGION;
import static uk.gov.moj.cp.config.aws.AwsSecretsEnvironmentPostProcessor.TMC_AWS_SECRET_NAME;

@ExtendWith(MockitoExtension.class)
class AwsSecretsEnvironmentPostProcessorTest {

    @Mock
    private Log log;

    /** postProcessEnvironment requires these before it calls loadSecret. */
    private static Map<String, Object> postProcessProps(String secretName, String region) {
        Map<String, Object> props = new HashMap<>();
        props.put(TMC_AWS_ENABLED, "true");
        props.put(TMC_AWS_SECRET_NAME, secretName);
        props.put(TMC_AWS_REGION, region);
        return props;
    }

    @Test
    void postProcessEnvironment_addsPropertySource_whenLoadSecretReturnsTmcKeys() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(Map.of("TMC_DB_URL", "jdbc:postgresql://mock/db"))
            .when(processor)
            .loadSecret(eq("my-secret"), eq("eu-west-2"));
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
            new MapPropertySource("testProps", postProcessProps("my-secret", "eu-west-2"))
        );

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getProperty("TMC_DB_URL")).isEqualTo("jdbc:postgresql://mock/db");
        assertThat(env.getPropertySources().get("awsSecretsManager"))
            .isInstanceOf(MapPropertySource.class);

        verify(processor).loadSecret(eq("my-secret"), eq("eu-west-2"));
    }

    @Test
    void postProcessEnvironment_addsPropertySource_whenSecretNameOnlyFromYamlProperty() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(Map.of("TMC_APP", "from-yaml-name"))
            .when(processor)
            .loadSecret(eq("yaml-secret-id"), eq("eu-west-2"));
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
            new MapPropertySource("testProps", postProcessProps("yaml-secret-id", "eu-west-2"))
        );

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getProperty("TMC_APP")).isEqualTo("from-yaml-name");
        verify(processor).loadSecret(eq("yaml-secret-id"), eq("eu-west-2"));
    }

    @Test
    void postProcessEnvironment_higherPrecedencePropertySourceWinsForSecretName() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(Map.of("TMC_ONE", "1"))
            .when(processor)
            .loadSecret(eq("env-secret"), eq("eu-west-2"));
        StandardEnvironment env = new StandardEnvironment();
        // addFirst: last addFirst is searched first, so "env" wins for tmc.aws.secret-name
        env.getPropertySources().addFirst(
            new MapPropertySource("yamlFirst", postProcessProps("yaml-secret", "eu-west-2"))
        );
        env.getPropertySources().addFirst(
            new MapPropertySource("envWins", postProcessProps("env-secret", "eu-west-2"))
        );

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getProperty("TMC_ONE")).isEqualTo("1");
        assertThat(env.getProperty(TMC_AWS_SECRET_NAME)).isEqualTo("env-secret");
        verify(processor).loadSecret(eq("env-secret"), eq("eu-west-2"));
        verify(processor, never()).loadSecret(eq("yaml-secret"), anyString());
    }

    @Test
    void postProcessEnvironment_resolvesRegionFromTmcAwsRegion() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(Map.of("TMC_REG", "ok"))
            .when(processor)
            .loadSecret(eq("s"), eq("some-eu-west-1"));
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
            new MapPropertySource("testProps", postProcessProps("s", "some-eu-west-1"))
        );

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getProperty("TMC_REG")).isEqualTo("ok");
        verify(processor).loadSecret(eq("s"), eq("some-eu-west-1"));
    }

    @Test
    void postProcessEnvironment_doesNotAddAwsSource_whenLoadSecretReturnsEmptyMap() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(Map.of())
            .when(processor)
            .loadSecret(eq("my-secret"), eq("eu-west-2"));
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
            new MapPropertySource("testProps", postProcessProps("my-secret", "eu-west-2"))
        );

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getPropertySources().get("awsSecretsManager")).isNull();
        verify(processor).loadSecret(eq("my-secret"), eq("eu-west-2"));
    }

    @Test
    void postProcessEnvironment_doesNotAddAwsSource_whenOnlyNonTmcKeysInSecret() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(Map.of("OTHER_KEY", "value"))
            .when(processor)
            .loadSecret(eq("my-secret"), eq("eu-west-2"));
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
            new MapPropertySource("testProps", postProcessProps("my-secret", "eu-west-2"))
        );

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getPropertySources().get("awsSecretsManager")).isNull();
        verify(postLog).warn(argThat(msg -> msg != null && msg.toString().contains("no TMC-prefixed keys")));
    }

    @Test
    void postProcessEnvironment_mapsMultipleTmcKeysFromSecret() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(
            Map.of(
                "TMC_DB_URL", "jdbc:a",
                "TMC_TOKEN", "token-value"))
            .when(processor)
            .loadSecret(eq("my-secret"), eq("eu-west-2"));
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
            new MapPropertySource("testProps", postProcessProps("my-secret", "eu-west-2"))
        );

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getProperty("TMC_DB_URL")).isEqualTo("jdbc:a");
        assertThat(env.getProperty("TMC_TOKEN")).isEqualTo("token-value");
        MapPropertySource awsSource = (MapPropertySource) env.getPropertySources().get("awsSecretsManager");
        assertThat(awsSource.getProperty("TMC_DB_URL")).isEqualTo("jdbc:a");
        assertThat(awsSource.getProperty("TMC_TOKEN")).isEqualTo("token-value");
    }

    @Test
    void postProcessEnvironment_tmcSecretOverridesExistingPropertyWithSameKey() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        Log postLog = mock(Log.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(postLog);

        AwsSecretsEnvironmentPostProcessor processor = spy(new AwsSecretsEnvironmentPostProcessor(logFactory));
        doReturn(Map.of("TMC_DB_URL", "jdbc:new"))
            .when(processor)
            .loadSecret(eq("my-secret"), eq("eu-west-2"));
        StandardEnvironment env = new StandardEnvironment();
        Map<String, Object> props = postProcessProps("my-secret", "eu-west-2");
        props.put("TMC_DB_URL", "jdbc:old");
        env.getPropertySources().addFirst(new MapPropertySource("testProps", props));

        processor.postProcessEnvironment(env, mock(SpringApplication.class));

        assertThat(env.getProperty("TMC_DB_URL")).isEqualTo("jdbc:new");
    }

    // ——— loadSecret (instance method, SecretsManagerClient mocked) ———

    private AwsSecretsEnvironmentPostProcessor processorForLoadSecret() {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        when(logFactory.getLog(any(Class.class))).thenReturn(log);
        return new AwsSecretsEnvironmentPostProcessor(logFactory);
    }

    @Test
    void loadSecret_emptySecretName_returnsEmptyWithoutTouchingClient() {
        AwsSecretsEnvironmentPostProcessor processor = processorForLoadSecret();
        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            assertThat(processor.loadSecret("", "eu-west-2")).isEmpty();
            assertThat(processor.loadSecret(null, "eu-west-2")).isEmpty();

            sm.verifyNoInteractions();
        }
    }

    @Test
    void loadSecret_withRegion_usesBuilderAndReturnsParsedJson() {
        AwsSecretsEnvironmentPostProcessor processor = processorForLoadSecret();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(
                GetSecretValueResponse.builder().secretString("{\"TMC_A\":\"v1\",\"TMC_B\":\"v2\"}").build());

        SecretsManagerClientBuilder builder = mock(SecretsManagerClientBuilder.class);
        when(builder.region(any(Region.class))).thenReturn(builder);
        when(builder.build()).thenReturn(client);

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::builder).thenReturn(builder);

            Map<String, String> out = processor.loadSecret("my-secret", "eu-west-2");
            assertThat(out).containsEntry("TMC_A", "v1").containsEntry("TMC_B", "v2");
        }
        verify(client).getSecretValue(any(GetSecretValueRequest.class));
        verify(log).info(contains("AWS Secrets: Loaded 2 keys"));
    }

    @Test
    void loadSecret_noRegion_usesCreate() {
        AwsSecretsEnvironmentPostProcessor processor = processorForLoadSecret();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("{\"K\":\"1\"}").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(processor.loadSecret("s", null)).containsEntry("K", "1");
            assertThat(processor.loadSecret("s", "")).containsEntry("K", "1");
        }
    }

    @Test
    void loadSecret_createPath_verifiedAgainstBuilder() {
        AwsSecretsEnvironmentPostProcessor processor = processorForLoadSecret();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("{}").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            processor.loadSecret("s", null);

            sm.verify(SecretsManagerClient::create, atLeastOnce());
            sm.verify(SecretsManagerClient::builder, never());
        }
    }

    @Test
    void loadSecret_emptySecretString_warnsAndReturnsEmpty() {
        AwsSecretsEnvironmentPostProcessor processor = processorForLoadSecret();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(processor.loadSecret("s", null)).isEmpty();
            verify(log).warn(contains("Secret string empty"));
        }
    }

    @Test
    void loadSecret_malformedJson_logsErrorAndReturnsEmpty() {
        AwsSecretsEnvironmentPostProcessor processor = processorForLoadSecret();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("not json").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(processor.loadSecret("s", null)).isEmpty();
            verify(log).error(contains("Failed to load secret"), any(Exception.class));
        }
    }

    @Test
    void loadSecret_getSecretValueThrows_logsErrorAndReturnsEmpty() {
        AwsSecretsEnvironmentPostProcessor processor = processorForLoadSecret();
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        doThrow(new RuntimeException("aws")).when(client).getSecretValue(any(GetSecretValueRequest.class));

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(processor.loadSecret("s", null)).isEmpty();
            verify(log).error(contains("Failed to load secret"), any(RuntimeException.class));
        }
    }
}
