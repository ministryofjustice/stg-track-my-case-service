package uk.gov.moj.cp.model;

public record OAuthTokenResponse(
    String token_type,
    int expires_in,
    int ext_expires_in,
    String access_token
) {}
