package uk.gov.moj.cp.client.api;

import com.moj.generated.hmcts.ProsecutionCase;
import org.springframework.http.ResponseEntity;

public interface ProsecutionCaseClient {

    ResponseEntity<ProsecutionCase> getCaseDetails(
        String accessToken,
        String caseUrn
    );
}
