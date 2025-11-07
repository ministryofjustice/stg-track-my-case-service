package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.Address;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtRoom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.CourtHouseClient;
import uk.gov.moj.cp.dto.CourtHouseDto;

@Slf4j
@Service
public class CourtHouseService {

    @Autowired
    private CourtHouseClient courtHouseClient;

    public CourtHouseDto getCourtHouseById(String token, String id, String courtRoomId) {
        HttpEntity<CourtHouse> result = courtHouseClient.getCourtHouseById(token, id, courtRoomId);

        if (result == null || result.getBody() == null) {
            log.atError().log("Response body is null or empty");
            return null;
        }
        return convertToJudiciaryResult(result.getBody(), id, courtRoomId);
    }

    private CourtHouseDto convertToJudiciaryResult(CourtHouse courtHouseResult, String id, String courtRoomId) {
        return new CourtHouseDto(
                id,
                courtRoomId,
                courtHouseResult.getCourtHouseType() != null ? courtHouseResult.getCourtHouseType().value() : null,
                courtHouseResult.getCourtHouseCode(),
                courtHouseResult.getCourtHouseName(),
                getAddressDto(courtHouseResult.getAddress()),
                courtHouseResult.getCourtRoom() != null
                        ? courtHouseResult.getCourtRoom().stream()
                        .map(this::getCourtRoomDto)
                        .toList()
                        : null
        );
    }

    private CourtHouseDto.CourtRoomDto getCourtRoomDto(CourtRoom cr) {
        return new CourtHouseDto.CourtRoomDto(
            cr.getCourtRoomId(),
            cr.getCourtRoomName()
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
}

