package uk.gov.moj.cp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.moj.cp.util.ApiUtils;

@Slf4j
public class UsersAuthorizationInterceptor implements HandlerInterceptor {
    private final String requiredHeaderValue;

    public UsersAuthorizationInterceptor(String requiredHeaderValue) {
        this.requiredHeaderValue = requiredHeaderValue;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        final String fullAuthorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        log.atInfo().log("**** read from env ****** {}", requiredHeaderValue);

        if (StringUtils.isNotEmpty(fullAuthorizationHeader) && StringUtils.isNotEmpty(requiredHeaderValue)) {
            final String bearerTokenPrefix = ApiUtils.BEARER_TOKEN_PREFIX;
            if (fullAuthorizationHeader.startsWith(bearerTokenPrefix)) {
                final String authorizationHeader = fullAuthorizationHeader.substring(bearerTokenPrefix.length());
                log.atInfo().log("**** bearer ****** {}", authorizationHeader);
                if (requiredHeaderValue.equals(authorizationHeader)) {
                    return true;
                }
            }
        }
        response.setStatus(HttpStatus.SC_FORBIDDEN);
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        response.getWriter().write("{\"message\":\"You are not allowed to access this resource\"}");
        return false;
    }
}
