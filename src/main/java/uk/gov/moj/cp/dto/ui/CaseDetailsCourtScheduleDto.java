package uk.gov.moj.cp.dto.ui;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CaseDetailsCourtScheduleDto(
    @JsonProperty("hearings") List<CaseDetailsHearingDto> hearings
) {

}
