package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class CourtRoomDto{

    @JsonProperty("courtRoomId")
    private int courtRoomId;

    @JsonProperty("courtRoomName")
    String courtRoomName;
}
