package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.Address;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtHouse.CourtHouseType;
import com.moj.generated.hmcts.CourtRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.api.CourtHouseClient;
import uk.gov.moj.cp.dto.ui.CourtHouseDto;
import uk.gov.moj.cp.dto.ui.CourtRoomDto;
import uk.gov.moj.cp.dto.ui.AddressDto;

import java.util.List;

import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourtHouseService {

    private final CourtHouseClient courtHouseClient;

    public CourtHouseDto getCourtHouseById(String accessToken, String courtId, String courtRoomId) {
        HttpEntity<CourtHouse> result = courtHouseClient.getCourtHouseById(accessToken, courtId, courtRoomId);

        if (result == null || result.getBody() == null) {
            log.atError().log("Response body is null or empty");
            return null;
        }
        return convertToJudiciaryResult(result.getBody(), courtId, courtRoomId);
    }

    private CourtHouseDto convertToJudiciaryResult(CourtHouse courtHouseResult, String id, String courtRoomId) {
        CourtHouseType courtHouseType = courtHouseResult.getCourtHouseType();
        List<CourtRoomDto> courtRoomDtos = nonNull(courtHouseResult.getCourtRoom())
            ? courtHouseResult.getCourtRoom().stream()
            .map(this::getCourtRoomDto)
            .toList()
            : null;
        AddressDto addressDto = getAddressDto(courtHouseResult.getAddress());
        return new CourtHouseDto(
            id,
            courtRoomId,
            nonNull(courtHouseType) ? courtHouseType.value() : null,
            courtHouseResult.getCourtHouseCode(),
            courtHouseResult.getCourtHouseName(),
            addressDto,
            courtRoomDtos
        );
    }

    private CourtRoomDto getCourtRoomDto(CourtRoom cr) {
        return new CourtRoomDto(cr.getCourtRoomId(), cr.getCourtRoomName());
    }

    private AddressDto getAddressDto(Address address) {
        return new AddressDto(
            address.getAddress1(),
            address.getAddress2(),
            address.getAddress3(),
            address.getAddress4(),
            address.getPostalCode(),
            address.getCountry()
        );
    }
}

