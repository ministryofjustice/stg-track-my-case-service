package uk.gov.moj.cp.client.api;

import com.moj.generated.hmcts.CourtHouse;
import org.springframework.http.ResponseEntity;

public interface CourtHouseClient {

    ResponseEntity<CourtHouse> getCourtHouseById(
        String accessToken,
        String courtId,
        String courtRoomId
    );
}
