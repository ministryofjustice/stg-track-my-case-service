package uk.gov.moj.cp.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaseDetailsDto(
    String caseUrn,
    @JsonProperty("courtSchedule") List<CaseDetailsCourtScheduleDto> courtSchedule
) {
    public record CaseDetailsCourtScheduleDto(
        @JsonProperty("hearings") List<CaseDetailsHearingDto> hearings
    ) {
        public record CaseDetailsHearingDto(
            @JsonProperty("courtSittings") List<CaseDetailsCourtSittingDto> courtSittings,
            @JsonProperty("hearingId") String hearingId,
            @JsonProperty("hearingType") String hearingType,
            @JsonProperty("hearingDescription") String hearingDescription,
            @JsonProperty("listNote") String listNote
        ) {
            public record CaseDetailsCourtSittingDto(
                @JsonProperty("judiciaryId") String judiciaryId,
                @JsonProperty("sittingStart") String sittingStart,
                @JsonProperty("sittingEnd") String sittingEnd,
                @JsonProperty("courtHouse") CourtHouseDto courtHouse
            ) {}

        }

    }
}
