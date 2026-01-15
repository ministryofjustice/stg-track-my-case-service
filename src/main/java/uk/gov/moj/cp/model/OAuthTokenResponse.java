package uk.gov.moj.cp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuthTokenResponse(
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") int expiresIn,
    @JsonProperty("ext_expires_in") int extExpiresIn,
    @JsonProperty("access_token") String accessToken
) {}
