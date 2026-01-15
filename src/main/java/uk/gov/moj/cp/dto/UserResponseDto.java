package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.moj.cp.model.UserStatus;
import uk.gov.moj.cp.model.UserRole;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private String email;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime updated;
}
