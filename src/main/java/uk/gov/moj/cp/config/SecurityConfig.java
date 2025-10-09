package uk.gov.moj.cp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.moj.cp.service.UserService;

import java.util.List;

import static uk.gov.moj.cp.controllers.CaseDetailsController.PATH_API_CASE;
import static uk.gov.moj.cp.controllers.UserController.PATH_API_USERS;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    private final String usersAuthorizationHeader;
    private final UserService userService;

    public SecurityConfig(@Value("${services.users.authorization-header}") String usersAuthorizationHeader,
                          UserService userService) {
        this.usersAuthorizationHeader = usersAuthorizationHeader;
        this.userService = userService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UsersAuthorizationInterceptor(usersAuthorizationHeader))
            .addPathPatterns(List.of(
                PATH_API_USERS + "/**"
            ));
        registry.addInterceptor(new CaseAuthorizationInterceptor(userService))
            .addPathPatterns(List.of(
                PATH_API_CASE + "/**"
            ));
    }
}
