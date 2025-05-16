package uk.gov.moj.cp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.moj.generated.hmcts.Address;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtRoom;
import com.moj.generated.hmcts.VenueContact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.CourtHouseClient;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.util.Utils;

@Slf4j
@Service
public class CourtHouseService {

    @Autowired
    private CourtHouseClient courtHouseClient;

    public CourtHouseDto getCourtHouseById(String id) {
        HttpEntity<String> result = courtHouseClient.getCourtHouseById(id);

        if (result == null || result.getBody() == null || result.getBody().isEmpty()) {
            log.error("Response body is null or empty");
            return null;
        }

        try {
            CourtHouse courtHouseResult = Utils.convertJsonStringToType(
                result.getBody(),
                CourtHouse.class
            );
            return convertToJudiciaryResult(courtHouseResult, id);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CourtHouseDto convertToJudiciaryResult(CourtHouse courtHouseResult, String id) {
        return new CourtHouseDto(
                id,
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
            getVenueContactDto(cr.getVenueContact()),
            cr.getAddress() != null ? getAddressDto(cr.getAddress()) : null
        );
    }

    private CourtHouseDto.CourtRoomDto.AddressDto getAddressDto(Address address) {
        return new CourtHouseDto.CourtRoomDto.AddressDto(
            address.getAddress1(),
            address.getAddress2(),
            address.getAddress3(),
            address.getAddress4(),
            address.getPostalCode(),
            address.getCountry()
        );
    }

    private CourtHouseDto.CourtRoomDto.VenueContactDto getVenueContactDto(VenueContact venueContact) {
        return new CourtHouseDto.CourtRoomDto.VenueContactDto(
            venueContact.getVenueTelephone(),
            venueContact.getVenueEmail(),
            venueContact.getPrimaryContactName(),
            venueContact.getVenueSupport()
        );
    }
}

