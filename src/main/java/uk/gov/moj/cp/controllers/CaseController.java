package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.CaseJudiciaryResult;
import uk.gov.moj.cp.service.CaseService;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;


/**
 * Default endpoints per application.
 */
@RestController
public class CaseController {

    /**
     * Case GET endpoint.
     *
     * @return List of CaseJudiciaryResult.
     */

    @Autowired
    private CaseService caseService;

    @GetMapping("cases/{id}/results")
    public ResponseEntity<List<CaseJudiciaryResult>> getCaseById(@PathVariable Long id) {
        return ok(caseService.getCaseById(id));
    }

}
