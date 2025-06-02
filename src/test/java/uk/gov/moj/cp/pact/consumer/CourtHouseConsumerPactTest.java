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
@PactTestFor(providerName = "CourtHouseProvider", pactVersion = au.com.dius.pact.core.model.PactSpecVersion.V4)
public class CourtHouseConsumerPactTest {

    @Pact(consumer = "CourtHouseConsumer")
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


/*
 LambdaDsl.newJsonBody(body -> {
                    body.stringType("courtHouseType", "crown");
                    body.stringType("courtHouseCode", "LND001");
                    body.stringType("courtHouseName", "Central London County Court");
                    body.stringType("courtHouseDescription", "Main Crown Court in London handling major cases");

                    body.minArrayLike("courtRoom", 1, courtRoom -> {
                        courtRoom.numberType("courtRoomNumber", 1);
                        courtRoom.numberType("courtRoomId", 101);
                        courtRoom.stringType("courtRoomName", "Courtroom A");

                        courtRoom.object("venueContact", venue -> {
                            venue.stringType("venueTelephone", "01772 844700");
                            venue.stringType("venueEmail", "court1@moj.gov.uk");
                            venue.stringType("primaryContactName", "Name");
                            venue.stringType("venueSupport", "0330 566 5561");
                        });

                        courtRoom.object("address", address -> {
                            address.stringType("address1", "Thomas More Building");
                            address.stringType("address2", "Royal Courts of Justice");
                            address.stringType("address3", "Strand");
                            address.stringType("address4", "London");
                            address.stringType("postalCode", "WC2A 2LL");
                            address.stringType("country", "UK");
                        });
                    });
                }).build()
 */
