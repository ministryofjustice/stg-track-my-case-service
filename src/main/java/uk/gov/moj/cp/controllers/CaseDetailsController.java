package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.service.CaseDetailsService;

import static org.springframework.http.ResponseEntity.ok;


/**
 * Default endpoints per application.
 */
@RestController
@RequestMapping(CaseDetailsController.PATH_API_CASE)
public class CaseDetailsController {

    public static final String PATH_API_CASE = "/case";

    /**
     * Judges GET endpoint.
     *
     * @return Judges params.
     */

    @Autowired
    private CaseDetailsService caseDetailsService;

    @GetMapping("/{case_urn}/casedetails")
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
