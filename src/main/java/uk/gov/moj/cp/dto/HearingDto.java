package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HearingDto {

    private String hearingId;
    private String hearingType;
    private String hearingDescription;
    private String listNote;

    @JsonProperty("weekCommencing")
    private WeekCommencingDto weekCommencing;

    @JsonProperty("courtSittings")
    private List<CourtSittingDto> courtSittings;

}
