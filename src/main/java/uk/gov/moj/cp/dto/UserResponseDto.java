package uk.gov.moj.cp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
    private String email;
    private String status;
    private String reason;

    public UserResponseDto(final String email, final String status, final String reason) {
        this.email = email;
        this.status = status;
        this.reason = reason;
    }

    public UserResponseDto(final String email, final String status) {
        this(email, status, null);
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
