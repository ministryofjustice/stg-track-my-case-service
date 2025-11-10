package uk.gov.moj.cp.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.config.ApiPaths;
import uk.gov.moj.cp.service.CaseDetailsService;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.http.ResponseEntity.ok;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping(ApiPaths.PATH_API_CASES)
public class CaseDetailsController {

    private final CaseDetailsService caseDetailsService;

    @GetMapping("/{case_urn}/casedetails")
    public ResponseEntity<?> getCaseDetailsByCaseUrn(@PathVariable("case_urn") String caseUrn) {
        log.atInfo().log("Received request to get case details for caseUrn: {}", caseUrn);
        try {
            return ok(caseDetailsService.getCaseDetailsByCaseUrn(caseUrn));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("An error occurred while processing the request either caseUrn is not available,"
                          + "or see the logs for more details.");
        }
    }
}
