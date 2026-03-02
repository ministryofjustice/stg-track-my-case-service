package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WeekCommencingDto{

    @JsonProperty("courtHouse")
    String courtHouse;

    @JsonProperty("startDate")
    String startDate;

    @JsonProperty("endDate")
    String endDate;

    @JsonProperty("durationInWeeks")
    int durationInWeeks;
}



