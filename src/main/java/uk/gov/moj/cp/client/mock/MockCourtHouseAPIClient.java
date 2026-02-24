package uk.gov.moj.cp.client.mock;

import com.moj.generated.hmcts.Address;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtRoom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.moj.cp.client.api.CourtHouseClient;

import java.util.List;

@Component
@ConditionalOnProperty(
    name = "services.use-mock-data",
    havingValue = "true"
)
public class MockCourtHouseAPIClient implements CourtHouseClient {

    public ResponseEntity<CourtHouse> getCourtHouseById(String accessToken,
                                                        String caseUrn,
                                                        String courtId,
                                                        String courtRoomId) {

        CourtHouse courtHouse = new CourtHouse(
            CourtHouse.CourtHouseType.MAGISTRATE,
            "B01IX00",
            "Westminster Magistrates' Court",
            new Address(
                "181 Marylebone Road",
                "London",
                null,
                null,
                "NW1 5BR",
                "UK"
            ),
            List.of(new CourtRoom(2975, "Courtroom 01"))
        );

        return ResponseEntity.ok(courtHouse);

    }
}
