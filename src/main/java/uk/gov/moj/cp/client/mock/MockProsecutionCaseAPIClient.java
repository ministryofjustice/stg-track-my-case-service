package uk.gov.moj.cp.client.mock;

import com.moj.generated.hmcts.ProsecutionCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.moj.cp.client.api.ProsecutionCaseClient;

@Component
@ConditionalOnProperty(
    name = "services.use-mock-data",
    havingValue = "true"
)
public class MockProsecutionCaseAPIClient implements ProsecutionCaseClient {
    @Override
    public ResponseEntity<ProsecutionCase> getCaseDetails(String accessToken, String caseUrn) {
        ProsecutionCase prosecutionCase = new ProsecutionCase("ACTIVE", false);
        return ResponseEntity.ok(prosecutionCase);
    }
}
