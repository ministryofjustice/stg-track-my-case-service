package uk.gov.moj.cp.dto.ui;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CaseDetailsCourtSittingDto(
    @JsonProperty("judiciaryId") String judiciaryId,
    @JsonProperty("sittingStart") String sittingStart,
    @JsonProperty("sittingEnd") String sittingEnd,
    @JsonProperty("courtHouse") CourtHouseDto courtHouse
) {
}
