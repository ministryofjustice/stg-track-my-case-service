package uk.gov.moj.cp.dto;

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
    @JsonProperty("address") CourtRoomDto.AddressDto address,
    //String courtHouseDescription,
    @JsonProperty("courtRoom") List<CourtRoomDto> courtRoomDtoList
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CourtRoomDto(
        int courtRoomId,
        String courtRoomName
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record AddressDto(
            String address1,
            String address2,
            String address3,
            String address4,
            String postalCode,
            String country
        ) {}
    }
}
