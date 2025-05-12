package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.service.CourtHouseService;

import static org.springframework.http.ResponseEntity.ok;


/**
 * Default endpoints per application.
 */
@RestController
public class CourtHouseController {

    /**
     * Judges GET endpoint.
     *
     * @return Judges params.
     */

    @Autowired
    private CourtHouseService courtHouseService;

    @GetMapping("courthouses/{id}")
    public ResponseEntity<CourtHouseDto> getCourtHouseById(@PathVariable Long id) {
        return ok(courtHouseService.getCourtHouseById(id));
    }

}
