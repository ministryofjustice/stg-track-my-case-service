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
@PactTestFor(providerName = "CPCourtScheduleProvider", pactVersion = au.com.dius.pact.core.model.PactSpecVersion.V4)
public class CourtScheduleConsumerPactTest {

    @Pact(consumer = "VPCourtScheduleConsumer")
    public V4Pact definePact(PactBuilder builder) throws IOException {

        JsonNode json = objectMapper
            .readTree(Paths.get("src/pactContractTest/resources/courtSchedule.json").toFile());

        return builder
            .usingLegacyDsl()
            .given("court schedule for case 456789 exists")
            .uponReceiving("A request to get court schedule for case 456789")
            .path("/case/456789/courtschedule")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(PactDslHelper.fromJson(json))
            .toPact(V4Pact.class);
    }

    @Test
    void testGetCourtSchedule(MockServer mockServer) {
        String url = mockServer.getUrl() + "/case/456789/courtschedule";
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        assertNotNull(response);
        assertTrue(response.contains("Initial appearance for case 456789"));
    }
}
