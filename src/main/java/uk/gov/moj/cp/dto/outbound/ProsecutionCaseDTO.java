package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"caseStatus", "reportingRestrictions"})
public class ProsecutionCaseDTO {

    @JsonProperty("caseStatus")
    String caseStatus;

    @JsonProperty("reportingRestrictions")
    boolean reportingRestrictions;
}

