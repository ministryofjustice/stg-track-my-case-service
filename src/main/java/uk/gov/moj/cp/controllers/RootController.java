package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.actuate.health.HealthEndpoint;

import static org.springframework.http.ResponseEntity.ok;



/**
 * Default endpoints per application.
 */
@RestController
public class RootController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    /**
     * Root GET endpoint.
     *
     * <p>MOJ application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on. This is the endpoint to deal with that and therefore silence the
     * unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to stg-track-my-case-service");
    }

    @GetMapping("/healthz")
    public ResponseEntity<?> healthz() {
        return ResponseEntity.ok(healthEndpoint.health());
    }
}
