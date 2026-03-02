package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"caseUrn", "courtSchedule"})
public class CaseDetailsDto {

    @JsonProperty("caseUrn")
    String caseUrn;

    @JsonProperty("courtSchedule")
    List<CaseDetailsCourtScheduleDto> courtSchedules;
}
