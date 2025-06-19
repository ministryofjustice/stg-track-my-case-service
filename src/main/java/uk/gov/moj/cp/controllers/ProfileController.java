package uk.gov.moj.cp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.ProfileDto;
import uk.gov.moj.cp.service.ProfileService;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;


/**
 * Default endpoints per application.
 */
@RestController
public class ProfileController {

    /**
     * Case GET endpoint.
     *
     * @return List of CaseJudiciaryResult.
     */

    @Autowired
    private ProfileService profileService;

    @GetMapping("cases/{id}")
    public ResponseEntity<List<ProfileDto>> profile(@PathVariable String id) {
        return ok(profileService.profile(id));
    }

}
