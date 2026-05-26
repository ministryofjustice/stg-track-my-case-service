package uk.gov.moj.cp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.oauth.OAuthTokenClient;
import uk.gov.moj.cp.model.AmpApiType;
import uk.gov.moj.cp.model.CachedToken;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthTokenService {

    private final OAuthTokenClient oauthTokenClient;

    @Value("${services.oauth-token.cache.token-cache-ttl-minutes}")
    private long tokenCacheTtlMinutes;

    private final Map<AmpApiType, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public String getJwtToken(AmpApiType ampApiType) {
        CachedToken cached = tokenCache.get(ampApiType);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached token for API: {}", ampApiType);
            String token = cached.getToken();
            return token;
        }
        log.info("Fetching new token for API: {}", ampApiType);
        String token = oauthTokenClient.getJwtToken(ampApiType).accessToken();
        tokenCache.put(ampApiType, new CachedToken(token, tokenCacheTtlMinutes));
        return token;
    }

    public void evictAllTokenCaches() {
        log.info("Evicting all OAuth token caches");
        tokenCache.clear();
    }

}
