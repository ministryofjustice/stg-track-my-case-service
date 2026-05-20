package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CourtSittingDto {

    @JsonProperty("sittingStart")
    String sittingStart;

    @JsonProperty("sittingEnd")
    String sittingEnd;

    @JsonProperty("judiciaryId")
    String judiciaryId;

    @JsonProperty("courtHouse")
    String courtHouse;

    @JsonProperty("courtRoom")
    String courtRoom;
}


