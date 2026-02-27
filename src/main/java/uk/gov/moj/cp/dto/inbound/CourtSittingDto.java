package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourtSittingDto{

    @JsonProperty("sittingStart")
    private String sittingStart;

    @JsonProperty("sittingEnd")
    private String sittingEnd;

    @JsonProperty("judiciaryId")
    private String judiciaryId;

    @JsonProperty("courtHouse")
    private String courtHouse;

    @JsonProperty("courtRoom")
    private String courtRoom;
}


