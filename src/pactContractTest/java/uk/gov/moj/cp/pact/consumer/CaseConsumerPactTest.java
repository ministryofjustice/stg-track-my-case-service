package uk.gov.moj.cp.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;
import uk.gov.moj.cp.pact.helper.PactDslHelper;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cp.util.Utils.objectMapper;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "CPHearingCasesProvider", pactVersion = au.com.dius.pact.core.model.PactSpecVersion.V4)
public class CaseConsumerPactTest {

    @Pact(consumer = "VPHearingCasesConsumer")
    public V4Pact definePact(PactBuilder builder) throws IOException {

        JsonNode json = objectMapper
            .readTree(Paths.get("src/pactContractTest/resources/case.json").toFile());

        return builder
            .usingLegacyDsl()
            .given("case with ID 123 exists")
            .uponReceiving("A request to get case results for case ID 123")
            .path("/cases/123/results")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(PactDslHelper.fromJson(json))
            .toPact(V4Pact.class);
    }

    @Test
    void testGetCase(MockServer mockServer) {
        String url = mockServer.getUrl() + "/cases/123/results";
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        assertNotNull(response);
        assertTrue(response.contains("This is the example outcome of case results"));
    }
}
