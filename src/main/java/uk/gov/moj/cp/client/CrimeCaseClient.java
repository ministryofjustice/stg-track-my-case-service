package uk.gov.moj.cp.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CrimeCaseClient {
    private final String crimeCaseUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-cases/0.0.1/cases/%s/results";
    private static final HttpHeaders headers = new HttpHeaders();
    private final RestTemplate restTemplate;

    @Autowired
    public CrimeCaseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public HttpEntity<String> getCaseById(Long id) {
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            String.format(crimeCaseUrl, id),
            HttpMethod.GET,
            entity,
            String.class
        );
        return response;
    }

}
