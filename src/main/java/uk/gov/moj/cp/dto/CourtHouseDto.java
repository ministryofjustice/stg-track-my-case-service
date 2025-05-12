package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CourtHouseDto(
    String courtHouseType,
    String courtHouseCode,
    String courtHouseName,
    String courtHouseDescription,
    @JsonProperty("courtRoomDtoList") List<CourtRoomDto> courtRoomDtoList
) {
    public record CourtRoomDto(
        int courtRoomNumber,
        int courtRoomId,
        String courtRoomName,
        @JsonProperty("venueContact") CourtRoomDto.VenueContactDto venueContact
    ) {
        public record VenueContactDto(
            String venueTelephone,
            String venueEmail,
            String primaryContactName,
            String venueSupport
        ) {}

    }
}
