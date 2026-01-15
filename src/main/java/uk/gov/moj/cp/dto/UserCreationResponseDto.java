package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.moj.cp.model.UserCreationStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor
@Builder
public class UserCreationResponseDto {
    private String email;
    private UserCreationStatus status;
    private String reason;
}
