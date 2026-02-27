package uk.gov.moj.cp.dto.ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourtHouseDto(
    String courtHouseId,
    String courtRoomId,
    String courtHouseType,
    String courtHouseCode,
    String courtHouseName,
    @JsonProperty("address") AddressDto address,
    //String courtHouseDescription,
    @JsonProperty("courtRoom") List<CourtRoomDto> courtRoomDtoList
) {

    public static CourtHouseDto withCourtHouseIdAndCourtRoomId(String courtHouseId, String courtRoomId) {
        return new CourtHouseDto(
            courtHouseId,
            courtRoomId,
            "", "", "", null, null
        );
    }

    public static CourtHouseDto withCourtHouseId(String courtHouseId) {
        return new CourtHouseDto(
            courtHouseId,
            "", "", "", "", null, null
        );
    }
}
