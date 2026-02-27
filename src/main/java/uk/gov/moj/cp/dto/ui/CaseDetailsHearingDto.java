package uk.gov.moj.cp.dto.ui;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CaseDetailsHearingDto(
    @JsonProperty("courtSittings") List<CaseDetailsCourtSittingDto> courtSittings,
    @JsonProperty("hearingId") String hearingId,
    @JsonProperty("hearingType") String hearingType,
    @JsonProperty("hearingDescription") String hearingDescription,
    @JsonProperty("listNote") String listNote,
    @JsonProperty("weekCommencing") CaseDetailsWeekCommencingDto weekCommencing
) {

}
