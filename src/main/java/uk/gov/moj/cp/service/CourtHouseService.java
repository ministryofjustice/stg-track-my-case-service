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
import uk.gov.moj.cp.dto.outbound.CourtHouseDto;
import uk.gov.moj.cp.dto.outbound.CourtRoomDto;
import uk.gov.moj.cp.dto.outbound.AddressDto;

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

    private CourtHouseDto convertToJudiciaryResult(CourtHouse courtHouse, String id, String courtRoomId) {
        CourtHouseType courtHouseType = courtHouse.getCourtHouseType();
        List<CourtRoomDto> courtRoomDtos = nonNull(courtHouse.getCourtRoom())
            ? courtHouse.getCourtRoom().stream()
            .map(this::getCourtRoomDto)
            .toList()
            : null;
        AddressDto addressDto = getAddressDto(courtHouse.getAddress());

       return CourtHouseDto.builder()
           .courtHouseId(id)
           .courtRoomId(courtRoomId)
           .courtHouseType(nonNull(courtHouseType) ? courtHouseType.value() : null)
           .courtHouseCode(courtHouse.getCourtHouseCode())
           .courtHouseName(courtHouse.getCourtHouseName())
           .address(addressDto)
           .courtRooms(courtRoomDtos)
           .build();
    }

    private CourtRoomDto getCourtRoomDto(CourtRoom courtRoom) {
        return CourtRoomDto.builder()
            .courtRoomId(courtRoom.getCourtRoomId())
            .courtRoomName(courtRoom.getCourtRoomName())
            .build();
    }

    private AddressDto getAddressDto(Address address) {
        return AddressDto.builder()
            .address1(address.getAddress1())
            .address2(address.getAddress2())
            .address3(address.getAddress3())
            .address4(address.getAddress4())
            .postalCode(address.getPostalCode())
            .country(address.getCountry())
            .build();
    }
}

