package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtRoomDto{

    @JsonProperty("courtRoomId")
    int courtRoomId;

    @JsonProperty("courtRoomName")
    String courtRoomName;
}
