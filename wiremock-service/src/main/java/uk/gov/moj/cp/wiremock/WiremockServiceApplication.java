package uk.gov.moj.cp.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;

public class WiremockServiceApplication {

    private static final String JSON = "application/json";

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("WIREMOCK_PORT", "8089"));

        WireMockServer server = new WireMockServer(
            WireMockConfiguration.options()
                .port(port)
                .extensions(new CourtScheduleResponseTransformer())
        );

        server.start();

        // Client credentials (same host as AMP: override TMC_TOKEN_URL to this WireMock in docker-compose)
        server.stubFor(post(urlPathMatching("/[^/]+/oauth2/v2\\.0/token"))
                           .willReturn(aResponse()
                                           .withHeader("Content-Type", JSON)
                                           .withStatus(200)
                                           .withBody(
                                               """
                                               {
                                                 "token_type": "Bearer",
                                                 "expires_in": 3600,
                                                 "ext_expires_in": 3600,
                                                 "access_token": "wiremock-docker-mock-oauth-access-token"
                                               }
                                               """
                                           )));

        server.stubFor(get(urlPathMatching("/courthouses/[^/]+/courtrooms/[^/]+"))
                           .willReturn(aResponse()
                                           .withHeader("Content-Type", JSON)
                                           .withStatus(200)
                                           .withBody(
                                               """
                                               {
                                                 "courtHouseType": "magistrate",
                                                 "courtHouseCode": "B01IX00",
                                                 "courtHouseName": "Westminster Magistrates' Court",
                                                 "address": {
                                                   "address1": "181 Marylebone Road",
                                                   "address2": "London",
                                                   "postalCode": "NW1 5BR",
                                                   "country": "UK"
                                                 },
                                                 "courtRoom": [
                                                   { "courtRoomId": 2975, "courtRoomName": "Courtroom 01" }
                                                 ]
                                               }
                                               """
                                           )));

        server.stubFor(get(urlPathMatching("/courthouses/[^/]+"))
                           .willReturn(aResponse()
                                           .withHeader("Content-Type", JSON)
                                           .withStatus(200)
                                           .withBody(
                                               """
                                               {
                                                 "courtHouseType": "magistrate",
                                                 "courtHouseCode": "B01IX00",
                                                 "courtHouseName": "Westminster Magistrates' Court",
                                                 "address": {
                                                   "address1": "181 Marylebone Road",
                                                   "address2": "London",
                                                   "postalCode": "NW1 5BR",
                                                   "country": "UK"
                                                 },
                                                 "courtRoom": [
                                                   { "courtRoomId": 2975, "courtRoomName": "Courtroom 01" }
                                                 ]
                                               }
                                               """
                                           )));

        server.stubFor(get(urlPathTemplate("/pcd/cases/{case_urn}"))
                           .willReturn(aResponse()
                                           .withHeader("Content-Type", JSON)
                                           .withStatus(200)
                                           .withBody(
                                               """
                                               { "caseStatus": "ACTIVE", "reportingRestrictions": false }
                                               """
                                           )));

        server.stubFor(get(urlPathTemplate("/case/{case_urn}/courtschedule"))
                           .willReturn(aResponse()
                                           .withTransformers(CourtScheduleResponseTransformer.NAME)));

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("WireMock service running on port " + port);
    }
}

