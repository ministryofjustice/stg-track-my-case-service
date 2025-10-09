package uk.gov.moj.cp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

import static uk.gov.moj.cp.controllers.UserController.PATH_API_USERS;

public class UsersAuthorizationInterceptor implements HandlerInterceptor {
    private final String requiredHeaderValue;

    public UsersAuthorizationInterceptor(String requiredHeaderValue) {
        this.requiredHeaderValue = requiredHeaderValue;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!request.getRequestURI().startsWith(PATH_API_USERS)) {
            return true;
        }
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNotEmpty(requiredHeaderValue) && requiredHeaderValue.equals(authorizationHeader)) {
            return true;
        }
        response.setStatus(HttpStatus.SC_FORBIDDEN);
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        response.getWriter().write("{\"message\":\"You are not allowed to access this resource\"}");
        return false;
    }
}
