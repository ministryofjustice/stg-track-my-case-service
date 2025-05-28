package uk.gov.moj.cp.dto;


import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileDto(
    String crn,
    String offence
) {
}
