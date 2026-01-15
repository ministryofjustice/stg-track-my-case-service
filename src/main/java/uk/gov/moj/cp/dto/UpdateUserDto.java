package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.model.UserStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class UpdateUserDto {
    private String email;
    private UserStatus status;
    private UserRole role;
}
