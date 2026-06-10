package uk.gov.moj.cp.service.notifications;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final NotificationsProperties notificationProperties;

    private static final String USER_EMAIL = "email";
    private static final String CASE_URN = "caseurn";
    private static final String SERVICE_URL = "serviceUrl";

    @SneakyThrows
    public void sendUserSearchedForTheCaseNotification(final String userEmail, final String caseUrn) {
        if (StringUtils.isNoneEmpty(userEmail) && StringUtils.isNoneEmpty(caseUrn)) {
            final String sentToEmail = notificationProperties.getTargetEmail();
            final String serviceUrl = notificationProperties.getServiceUrl();
            emailService.sendMail(
                sentToEmail,
                notificationProperties.getUserLoggedInEmailTemplateId(),
                addProperties(userEmail, caseUrn, serviceUrl),
                null
            );
            log.info("A user {} searched for the case {}", userEmail, caseUrn);
        }
    }

    public Map<String, String> addProperties(String userEmail, String caseUrn, String serviceUrl) {
        Map<String, String> customProps = new HashMap<>();
        customProps.put(USER_EMAIL, userEmail);
        customProps.put(CASE_URN, caseUrn);
        customProps.put(SERVICE_URL, serviceUrl);
        return customProps;
    }
}
