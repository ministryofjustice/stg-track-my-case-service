package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtSittingDto(
    String sittingStart,
    String sittingEnd,
    @JsonProperty("judiciaryId") String judiciaryId,
    String courtHouse,
    String courtRoom
) {
}
