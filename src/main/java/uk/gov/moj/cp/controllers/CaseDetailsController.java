package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.CaseDetailsDto;
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
    public ResponseEntity<CaseDetailsDto> getCaseDetailsByCaseUrn(@PathVariable("case_urn") String caseUrn) {
        return ok(caseDetailsService.getCaseDetailsByCaseUrn(caseUrn));
    }

}
