package uk.gov.moj.cp.client.api;

import com.moj.generated.hmcts.CourtScheduleSchema;
import org.springframework.http.ResponseEntity;

public interface CourtScheduleClient {

    ResponseEntity<CourtScheduleSchema> getCourtScheduleByCaseUrn(
        String accessToken,
        String caseUrn);
}
