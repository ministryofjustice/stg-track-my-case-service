package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaseDetailsWeekCommencingDto{

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("durationInWeeks")
    private int durationInWeeks;

    @JsonProperty("courtHouse")
    private CourtHouseDto courtHouse;
}

