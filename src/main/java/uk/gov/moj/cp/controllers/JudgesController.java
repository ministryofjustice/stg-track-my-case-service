package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.JudgesResult;
import uk.gov.moj.cp.service.JudgesService;

import static org.springframework.http.ResponseEntity.ok;


/**
 * Default endpoints per application.
 */
@RestController
public class JudgesController {

    /**
     * Judges GET endpoint.
     *
     * @return Judges params.
     */

    @Autowired
    private JudgesService judgesService;

    @GetMapping("judges/{id}")
    public ResponseEntity<JudgesResult> getCaseById(@PathVariable Long id) {
        return ok(judgesService.getCaseById(id));
    }

}
