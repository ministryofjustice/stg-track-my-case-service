package uk.gov.moj.cp.dto;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtScheduleDto(
    @JsonProperty("hearings") List<HearingDto> hearingDtos
) {
    public record HearingDto(
        String hearingId,
        String hearingType,
        String hearingDescription,
        String listNote,
        String weekCommencingStartDate,
        String weekCommencingEndDate,
        String weekCommencingDurationInWeeks,
        @JsonProperty("courtSittings") List<CourtSittingDto> courtSittingDtos
    ) {
        public record CourtSittingDto(
            String sittingStart,
            String sittingEnd,
            @JsonProperty("judiciaryId") String judiciaryId,
            String courtHouse,
            String courtRoom
        ) {}
    }
}

