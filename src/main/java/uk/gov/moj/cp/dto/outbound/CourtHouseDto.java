package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtHouseDto {

    @JsonProperty("courtHouseId")
    String courtHouseId;

    @JsonProperty("courtRoomId")
    String courtRoomId;

    @JsonProperty("courtHouseType")
    String courtHouseType;

    @JsonProperty("courtHouseCode")
    String courtHouseCode;

    @JsonProperty("courtHouseName")
    String courtHouseName;

    @JsonProperty("address")
    AddressDto address;

    @JsonProperty("courtRoom")
    List<CourtRoomDto> courtRooms;

}

