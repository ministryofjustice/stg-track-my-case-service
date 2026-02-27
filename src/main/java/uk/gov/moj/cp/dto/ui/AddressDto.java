package uk.gov.moj.cp.dto.ui;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddressDto(
    String address1,
    String address2,
    String address3,
    String address4,
    String postalCode,
    String country
) {
}
