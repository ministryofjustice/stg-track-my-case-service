package uk.gov.moj.cp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.moj.cp.entity.User;
import uk.gov.moj.cp.model.UserStatus;
import uk.gov.moj.cp.service.UserService;
import uk.gov.moj.cp.util.ApiUtils;

import java.util.Base64;
import java.util.Optional;

@Slf4j
public class CaseAuthorizationInterceptor implements HandlerInterceptor {
    private final UserService userService;

    public CaseAuthorizationInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        try {
            final String fullAuthorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.isNotEmpty(fullAuthorizationHeader)) {
                final String basicTokenPrefix = ApiUtils.BASIC_TOKEN_PREFIX;
                if (fullAuthorizationHeader.startsWith(basicTokenPrefix)) {
                    final String userEncodedEmail = fullAuthorizationHeader.substring(basicTokenPrefix.length());
                    final String oneLoginEmail = new String(Base64.getDecoder().decode(userEncodedEmail));
                    Optional<User> userOptional = this.userService.getByEmailIgnoreCase(oneLoginEmail);
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        if (UserStatus.ACTIVE.equals(user.getStatus())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while decoding authorisation header", e);
        }
        response.setStatus(HttpStatus.SC_FORBIDDEN);
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        response.getWriter().write("{\"message\":\"You are not allowed to access this resource\"}");
        return false;
    }
}
