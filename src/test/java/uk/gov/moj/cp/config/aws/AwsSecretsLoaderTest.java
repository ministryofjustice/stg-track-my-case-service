package uk.gov.moj.cp.config.aws;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsSecretsLoaderTest {

    @Mock
    private Log log;

    @Test
    void loadSecret_emptySecretName_returnsEmptyWithoutTouchingClient() {
        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            assertThat(AwsSecretsLoader.loadSecret(log, "", "eu-west-2")).isEmpty();
            assertThat(AwsSecretsLoader.loadSecret(log, null, "eu-west-2")).isEmpty();

            sm.verifyNoInteractions();
        }
    }

    @Test
    void loadSecret_withRegion_usesBuilderAndReturnsParsedJson() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(
                GetSecretValueResponse.builder().secretString("{\"TMC_A\":\"v1\",\"TMC_B\":\"v2\"}").build());

        SecretsManagerClientBuilder builder = mock(SecretsManagerClientBuilder.class);
        when(builder.region(any(Region.class))).thenReturn(builder);
        when(builder.build()).thenReturn(client);

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::builder).thenReturn(builder);

            Map<String, String> out = AwsSecretsLoader.loadSecret(log, "my-secret", "eu-west-2");
            assertThat(out).containsEntry("TMC_A", "v1").containsEntry("TMC_B", "v2");
        }
        verify(client).getSecretValue(any(GetSecretValueRequest.class));
        verify(log).info(contains("AwsSecretsLoader: Loaded 2 keys"));
    }

    @Test
    void loadSecret_noRegion_usesCreate() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("{\"K\":\"1\"}").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(AwsSecretsLoader.loadSecret(log, "s", null)).containsEntry("K", "1");
            assertThat(AwsSecretsLoader.loadSecret(log, "s", "")).containsEntry("K", "1");
        }
    }

    @Test
    void loadSecret_createPath_verifiedAgainstBuilder() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("{}").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            AwsSecretsLoader.loadSecret(log, "s", null);

            sm.verify(SecretsManagerClient::create, atLeastOnce());
            sm.verify(SecretsManagerClient::builder, never());
        }
    }

    @Test
    void loadSecret_emptySecretString_warnsAndReturnsEmpty() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(AwsSecretsLoader.loadSecret(log, "s", null)).isEmpty();
            verify(log).warn(contains("Secret string empty"));
        }
    }

    @Test
    void loadSecret_malformedJson_logsErrorAndReturnsEmpty() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("not json").build());

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(AwsSecretsLoader.loadSecret(log, "s", null)).isEmpty();
            verify(log).error(contains("Failed to load secret"), any(Exception.class));
        }
    }

    @Test
    void loadSecret_getSecretValueThrows_logsErrorAndReturnsEmpty() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        doThrow(new RuntimeException("aws")).when(client).getSecretValue(any(GetSecretValueRequest.class));

        try (MockedStatic<SecretsManagerClient> sm = org.mockito.Mockito.mockStatic(SecretsManagerClient.class)) {
            sm.when(SecretsManagerClient::create).thenReturn(client);

            assertThat(AwsSecretsLoader.loadSecret(log, "s", null)).isEmpty();
            verify(log).error(contains("Failed to load secret"), any(RuntimeException.class));
        }
    }
}
