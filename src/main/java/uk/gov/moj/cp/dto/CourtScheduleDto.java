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
        @JsonProperty("weekCommencing") WeekCommencingDto weekCommencingDto,
        @JsonProperty("courtSittings") List<CourtSittingDto> courtSittingDtos
    ) {
        public record WeekCommencingDto(
            String courtHouse,
            String startDate,
            String endDate,
            int durationInWeeks
        ) {}

        public record CourtSittingDto(
            String sittingStart,
            String sittingEnd,
            @JsonProperty("judiciaryId") String judiciaryId,
            String courtHouse,
            String courtRoom
        ) {}
    }
}

