package uk.gov.moj.cp.service.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationClient notificationClient;

    public void sendMail(
        String targetEmail,
        String emailTemplate,
        Map<String, String> parameters,
        String reference
    ) throws NotificationClientException {
        notificationClient.sendEmail(emailTemplate, targetEmail, parameters, reference);
    }
}
