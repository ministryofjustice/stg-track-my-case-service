package uk.gov.moj.cp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtRoom;
import com.moj.generated.hmcts.VenueContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.CourtHouseClient;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.util.Utils;

import static uk.gov.moj.cp.util.Utils.getJsonNode;

@Service
public class CourtHouseService {

    @Autowired
    private CourtHouseClient courtHouseClient;

    public CourtHouseDto getCourtHouseById(Long id) {
        HttpEntity<String> result = courtHouseClient.getCourtHouseById(id);

        if (result.getBody() == null || result.getBody().isEmpty()) {
            throw new RuntimeException("Response body is null or empty");
        }

        try {
            JsonNode courtHouse = getJsonNode(result.getBody(), "courtHouse");
            CourtHouse courtHouseResult = Utils.convertJsonStringToType(
                courtHouse.toString(),
                CourtHouse.class
            );
            return convertToJudiciaryResult(courtHouseResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CourtHouseDto convertToJudiciaryResult(CourtHouse courtHouseResult) {
        return new CourtHouseDto(
            courtHouseResult.getCourtHouseType() != null ? courtHouseResult.getCourtHouseType().value() : null,
            courtHouseResult.getCourtHouseCode(),
            courtHouseResult.getCourtHouseName(),
            courtHouseResult.getCourtHouseDescription(),
            courtHouseResult.getCourtRoom() != null
                ? courtHouseResult.getCourtRoom().stream()
                .map(this::getCourtRoomDto)
                .toList()
                : null
        );
    }

    private CourtHouseDto.CourtRoomDto getCourtRoomDto(CourtRoom cr) {
        return new CourtHouseDto.CourtRoomDto(
            cr.getCourtRoomNumber(),
            cr.getCourtRoomId(),
            cr.getCourtRoomName(),
            getVenueContactDto(cr.getVenueContact())
        );
    }

    public CourtHouseDto.CourtRoomDto.VenueContactDto getVenueContactDto(VenueContact venueContact) {
        return new CourtHouseDto.CourtRoomDto.VenueContactDto(
            venueContact.getVenueTelephone(),
            venueContact.getVenueEmail(),
            venueContact.getPrimaryContactName(),
            venueContact.getVenueSupport()
        );
    }
}

