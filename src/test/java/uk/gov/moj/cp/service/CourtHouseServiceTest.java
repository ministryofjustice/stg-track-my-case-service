package uk.gov.moj.cp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static java.util.UUID.randomUUID;

import com.moj.generated.hmcts.Address;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtHouse.CourtHouseType;
import com.moj.generated.hmcts.CourtRoom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.moj.cp.client.CourtHouseClient;
import uk.gov.moj.cp.dto.CourtHouseDto;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CourtHouseServiceTest {

    @Mock
    private CourtHouseClient courtHouseClient;

    @InjectMocks
    private CourtHouseService courtHouseService;


    @Test
    void testGetCourtHouseByCourtHouseById_successfulCourtHouseDetails() {
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();

        final Address address = new Address(
            "1 High Street",
            "Court Road",
            "London",
            null,
            "AA1 2BB",
            "UK"
        );

        final CourtHouse courtHouse = new CourtHouse(
            CourtHouseType.CROWN,
            "CHC123",
            "Lavender Hill",
            address,
            List.of(new CourtRoom(10, "CourtRoom 10"), new CourtRoom(20, "CourtRoom 20"))
        );

        final ResponseEntity<CourtHouse> entity = ResponseEntity.ok(courtHouse);
        when(courtHouseClient.getCourtHouseById(courtHouseId, courtRoomId)).thenReturn(entity);

        CourtHouseDto dto = courtHouseService.getCourtHouseById(courtHouseId, courtRoomId);

        assertNotNull(dto);
        assertEquals(courtHouseId, dto.courtHouseId());
        assertEquals(courtRoomId, dto.courtRoomId());
        assertEquals("crown", dto.courtHouseType());
        assertEquals("CHC123", dto.courtHouseCode());
        assertEquals("Lavender Hill", dto.courtHouseName());

        assertNotNull(dto.address());
        assertEquals("1 High Street", dto.address().address1());
        assertEquals("Court Road", dto.address().address2());
        assertEquals("London", dto.address().address3());
        assertNull(dto.address().address4());
        assertEquals("AA1 2BB", dto.address().postalCode());
        assertEquals("UK", dto.address().country());

        assertNotNull(dto.courtRoomDtoList());
        assertEquals(2, dto.courtRoomDtoList().size());
        assertEquals(10, dto.courtRoomDtoList().get(0).courtRoomId());
        assertEquals("CourtRoom 10", dto.courtRoomDtoList().get(0).courtRoomName());
        assertEquals(20, dto.courtRoomDtoList().get(1).courtRoomId());
        assertEquals("CourtRoom 20", dto.courtRoomDtoList().get(1).courtRoomName());
    }


    @Test
    void testGetCourtHouseByCourtHouseById_returnsNull() {
        when(courtHouseClient.getCourtHouseById(anyString(), anyString()))
            .thenReturn(new ResponseEntity<>(null, null, 200));

        CourtHouseDto result = courtHouseService.getCourtHouseById("courtId", "courtRoomId");

        assertNull(result);
    }


}


