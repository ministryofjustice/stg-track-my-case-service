package uk.gov.moj.cp.dto.ui;

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

}
