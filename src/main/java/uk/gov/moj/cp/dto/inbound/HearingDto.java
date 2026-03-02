package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class HearingDto {

    @JsonProperty("hearingId")
    String hearingId;

    @JsonProperty("hearingType")
    String hearingType;

    @JsonProperty("hearingDescription")
    String hearingDescription;

    @JsonProperty("listNote")
    String listNote;

    @JsonProperty("weekCommencing")
    WeekCommencingDto weekCommencing;

    @JsonProperty("courtSittings")
    List<CourtSittingDto> courtSittings;

}
