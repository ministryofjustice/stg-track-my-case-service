package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.service.CaseDetailsService;

import static org.springframework.http.ResponseEntity.ok;


/**
 * Default endpoints per application.
 */
@RestController
public class CaseDetailsController {

    /**
     * Judges GET endpoint.
     *
     * @return Judges params.
     */

    @Autowired
    private CaseDetailsService caseDetailsService;

    @GetMapping("/case/{case_urn}/casedetails")
    public ResponseEntity<?> getCaseDetailsByCaseUrn(@PathVariable("case_urn") String caseUrn) {
        try {
            return ok(caseDetailsService.getCaseDetailsByCaseUrn(caseUrn));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("An error occurred while processing the request either caseUrn is not available. "
                          + "or see the logs for more details.");
        }
    }

}
