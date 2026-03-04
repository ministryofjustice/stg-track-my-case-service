package uk.gov.moj.cp.dto.inbound;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CourtScheduleDto {

    @JsonProperty("hearings")
    List<HearingDto> hearings;
}
