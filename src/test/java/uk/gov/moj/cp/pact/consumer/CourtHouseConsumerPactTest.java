package uk.gov.moj.cp.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;
import uk.gov.moj.cp.pact.helper.PactDslHelper;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "VPCourtHouseProvider1", pactVersion = au.com.dius.pact.core.model.PactSpecVersion.V4)
public class CourtHouseConsumerPactTest {

    @Pact(consumer = "VPCourtHouseConsumer1")
    public V4Pact definePact(PactBuilder builder) throws IOException {

        JsonNode json = new ObjectMapper()
            .readTree(Paths.get("src/test/resources/courtHouse.json").toFile());

        return builder
            .usingLegacyDsl() // Important: enables DSL compatibility
            .given("court house with ID 123 exists")
            .uponReceiving("A request to get court house details with ID 123")
            .path("/courthouses/123")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(PactDslHelper.fromJson(json))
            .toPact(V4Pact.class);
    }

    @Test
    void testGetCourtHouse(MockServer mockServer) {
        String url = mockServer.getUrl() + "/courthouses/123";
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        assertNotNull(response);
        assertTrue(response.contains("Central London County Court"));
    }
}
