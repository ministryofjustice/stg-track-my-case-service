package uk.gov.moj.cp.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.config.ApiPaths;

@RequiredArgsConstructor
@RestController
@RequestMapping(ApiPaths.PATH_API_CASES)
public class AccessibilityController {

    @GetMapping("/active-user")
    public ResponseEntity<?> isActiveUser() {
        // all logic is covered in CaseAuthorizationInterceptor
        // so if we came here, user should be ACTIVE
        return ResponseEntity.ok().build();
    }
}
