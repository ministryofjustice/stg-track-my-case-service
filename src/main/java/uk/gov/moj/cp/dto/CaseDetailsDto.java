package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "caseUrn", "courtSchedule" })
public record CaseDetailsDto(
    @JsonProperty("caseUrn") String caseUrn,
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
            @JsonProperty("listNote") String listNote,
            @JsonProperty("weekCommencingStartDate") String weekCommencingStartDate,
            @JsonProperty("weekCommencingEndDate") String weekCommencingEndDate,
            @JsonProperty("weekCommencingDurationInWeeks") String weekCommencingDurationInWeeks
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
