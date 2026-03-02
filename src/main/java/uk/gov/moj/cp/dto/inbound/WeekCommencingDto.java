package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeekCommencingDto{

    @JsonProperty("courtHouse")
    private String courtHouse;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("durationInWeeks")
    private int durationInWeeks;
}



