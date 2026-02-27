package uk.gov.moj.cp.dto.ui;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CaseDetailsWeekCommencingDto(
    @JsonProperty("startDate") String startDate,
    @JsonProperty("endDate") String endDate,
    @JsonProperty("durationInWeeks") int durationInWeeks,
    @JsonProperty("courtHouse") CourtHouseDto courtHouse
) {
}
