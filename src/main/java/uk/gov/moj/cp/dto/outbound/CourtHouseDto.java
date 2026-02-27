package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class CourtHouseDto{

    @JsonProperty("courtHouseId")
    private String courtHouseId;

    @JsonProperty("courtRoomId")
    private String courtRoomId;

    @JsonProperty("courtHouseType")
    private String courtHouseType;

    @JsonProperty("courtHouseCode")
    private String courtHouseCode;

    @JsonProperty("courtHouseName")
    private String courtHouseName;

    @JsonProperty("address")
    private AddressDto address;

    @JsonProperty("courtRoom")
    private List<CourtRoomDto> courtRooms;

}
