package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CaseDetailsHearingDto{

    @JsonProperty("hearingId")
    private String hearingId;

    @JsonProperty("hearingType")
    private String hearingType;

    @JsonProperty("hearingDescription")
    private String hearingDescription;

    @JsonProperty("listNote")
    private String listNote;

    @JsonProperty("courtSittings")
    private List<CaseDetailsCourtSittingDto> courtSittings;

    @JsonProperty("weekCommencing")
    private CaseDetailsWeekCommencingDto weekCommencing;

}
