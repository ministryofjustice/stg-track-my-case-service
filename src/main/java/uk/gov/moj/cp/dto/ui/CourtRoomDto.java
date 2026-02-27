package uk.gov.moj.cp.dto.ui;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourtRoomDto(
    int courtRoomId,
    String courtRoomName
) {
}
