package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaseDetailsCourtSittingDto{

    @JsonProperty("judiciaryId")
    private String judiciaryId;

    @JsonProperty("sittingStart")
    private String sittingStart;

    @JsonProperty("sittingEnd")
    private String sittingEnd;

    @JsonProperty("courtHouse")
    private CourtHouseDto courtHouse;
}


