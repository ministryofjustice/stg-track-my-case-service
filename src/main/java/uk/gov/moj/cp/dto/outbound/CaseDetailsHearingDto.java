package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseDetailsHearingDto{

    @JsonProperty("hearingId")
    String hearingId;

    @JsonProperty("hearingType")
    String hearingType;

    @JsonProperty("hearingDescription")
    String hearingDescription;

    @JsonProperty("listNote")
    String listNote;

    @JsonProperty("courtSittings")
    List<CaseDetailsCourtSittingDto> courtSittings;

    @JsonProperty("weekCommencing")
    CaseDetailsWeekCommencingDto weekCommencing;

}
