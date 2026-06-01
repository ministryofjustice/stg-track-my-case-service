package uk.gov.moj.cp.model;

import lombok.Getter;

import java.time.Instant;

public class CachedToken {

    @Getter
    private final String token;

    private final Instant expiresAt;

    public CachedToken(String token, long ttlMinutes) {
        this.token = token;
        this.expiresAt = Instant.now().plusSeconds(ttlMinutes * 60);
    }

    public boolean isExpired() {
        if (Instant.now().isAfter(expiresAt)) {
            return true;
        }
        return false;
    }
}
