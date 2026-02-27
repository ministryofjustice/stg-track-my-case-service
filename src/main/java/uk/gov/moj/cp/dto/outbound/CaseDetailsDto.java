package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"caseUrn", "courtSchedule"})
@Getter
@Builder
public class CaseDetailsDto {

    @JsonProperty("caseUrn")
    private String caseUrn;

    @JsonProperty("courtSchedule")
    List<CaseDetailsCourtScheduleDto> courtSchedules;
}
