package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CaseDetailsCourtScheduleDto{

    @JsonProperty("hearings")
    private List<CaseDetailsHearingDto> hearings;
}


