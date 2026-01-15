package uk.gov.moj.cp.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.config.ApiPaths;

@RequiredArgsConstructor
@RestController
@RequestMapping(ApiPaths.PATH_API)
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> getHealth() {
        return ResponseEntity.ok("UP");
    }
}
