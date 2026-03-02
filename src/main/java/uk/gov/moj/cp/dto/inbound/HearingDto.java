package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HearingDto {

    @JsonProperty("hearingId")
    private String hearingId;

    @JsonProperty("hearingType")
    private String hearingType;

    @JsonProperty("hearingDescription")
    private String hearingDescription;

    @JsonProperty("listNote")
    private String listNote;

    @JsonProperty("weekCommencing")
    private WeekCommencingDto weekCommencing;

    @JsonProperty("courtSittings")
    private List<CourtSittingDto> courtSittings;

}
