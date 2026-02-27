package uk.gov.moj.cp.dto;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtScheduleDto(
    @JsonProperty("hearings") List<HearingDto> hearingDtos
) {
}

