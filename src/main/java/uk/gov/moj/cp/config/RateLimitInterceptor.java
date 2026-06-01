package uk.gov.moj.cp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int rateLimitMaximumTokensCapacity;
    private final int rateLimitRefillTokens;
    private final int rateLimitRefillPeriodMinutes;

    private final Map<String, Bucket> userLimitBuckets = new ConcurrentHashMap<>();

    public RateLimitInterceptor(int rateLimitMaximumTokensCapacity, int rateLimitRefillTokens, int rateLimitRefillPeriodMinutes) {
        this.rateLimitMaximumTokensCapacity = rateLimitMaximumTokensCapacity;
        this.rateLimitRefillTokens = rateLimitRefillTokens;
        this.rateLimitRefillPeriodMinutes = rateLimitRefillPeriodMinutes;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String rateLimitKey = resolveRateLimitKey(request);
        log.info("Rate limit key [{}]", rateLimitKey);

        Bucket bucket = userLimitBuckets.computeIfAbsent(rateLimitKey, key -> newBucket());
        ConsumptionProbe consumptionProbe = bucket.tryConsumeAndReturnRemaining(1);
        if (consumptionProbe.isConsumed()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        long secondsWaitForRefill = consumptionProbe.getNanosToWaitForRefill() / 1_000_000_000;
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(secondsWaitForRefill));
        response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests - please retry after the Retry-After period");
        return false;
    }

    private Bucket newBucket() {
        Refill refill = Refill.greedy(rateLimitRefillTokens, Duration.ofMinutes(rateLimitRefillPeriodMinutes));
        Bandwidth limit = Bandwidth.classic(rateLimitMaximumTokensCapacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveRateLimitKey(HttpServletRequest request) {
        final String sessionId = request.getHeader("X-Session-Id");
        if (sessionId != null && !sessionId.isBlank()) {
            log.debug("Rate limiting by X-Session-Id header [{}]", sessionId);
            return sessionId;
        }
        log.error("X-Session-Id header not set, please check request on UI");

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            log.debug("Rate limiting by Authorization header [{}]", authHeader);
            return authHeader.split(" ")[1].trim();
        }
        log.debug("Rate limiting by remoteAddr [{}]", request.getRemoteAddr());
        return request.getRemoteAddr();
    }
}
