package uk.gov.moj.cp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.moj.cp.entity.User;
import uk.gov.moj.cp.model.UserStatus;
import uk.gov.moj.cp.service.UserService;

import java.util.Base64;
import java.util.Optional;

import static uk.gov.moj.cp.controllers.CaseDetailsController.PATH_API_CASE;

public class CaseAuthorizationInterceptor implements HandlerInterceptor {
    private final UserService userService;

    public CaseAuthorizationInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (request != null) {
            return true;
        }
        if (!request.getRequestURI().startsWith(PATH_API_CASE)) {
            return true;
        }
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isNotEmpty(authorizationHeader)) {
            final String userEncodedEmail = authorizationHeader.substring("Basic ".length());
            final String oneLoginEmail = new String(Base64.getDecoder().decode(userEncodedEmail));
            Optional<User> userOptional = this.userService.getByEmailIgnoreCase(oneLoginEmail);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (UserStatus.ACTIVE.equals(user.getStatus())) {
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
