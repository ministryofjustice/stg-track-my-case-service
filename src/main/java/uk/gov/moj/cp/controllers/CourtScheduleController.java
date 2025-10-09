package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.service.CourtScheduleService;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;


/**
 * Default endpoints per application.
 */
@RestController
public class CourtScheduleController {

    /**
     * Judges GET endpoint.
     *
     * @return Judges params.
     */

    @Autowired
    private CourtScheduleService courtScheduleService;

    @GetMapping("/case/{case_urn}/courtschedule")
    public ResponseEntity<List<CourtScheduleDto>> getCourtScheduleByCaseUrn(@PathVariable("case_urn") String caseUrn) {
        return ok(courtScheduleService.getCourtScheduleByCaseUrn(caseUrn));
    }

}
