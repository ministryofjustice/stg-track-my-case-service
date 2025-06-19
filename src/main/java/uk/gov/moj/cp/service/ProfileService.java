package uk.gov.moj.cp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.ProfileDto;
import uk.gov.moj.cp.util.Utils;

import java.util.List;

@Service
public class ProfileService {

    public List<ProfileDto> profile(String id) {
        String output = "[ { \"crn\": 1234567891011, \"offence\": \"Burglary\" }, "
            + "{ \"crn\":  1110987654321 , \"offence\": \"Assault\" } ]";

        try {
            List<ProfileDto> profileList = Utils.convertJsonStringToList(
                output,
                ProfileDto.class
            );
            return profileList;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
