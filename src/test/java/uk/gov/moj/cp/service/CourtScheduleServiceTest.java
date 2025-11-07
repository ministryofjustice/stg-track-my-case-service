package uk.gov.moj.cp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static java.util.UUID.randomUUID;

import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtScheduleSchema;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.moj.cp.client.CourtScheduleClient;
import uk.gov.moj.cp.dto.CourtScheduleDto;

import java.util.Date;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CourtScheduleServiceTest {

    @Mock
    private CourtScheduleClient courtScheduleClient;

    @InjectMocks
    private CourtScheduleService courtScheduleService;
    private final String accessToken = "testToken";


    @Test
    void testGetScheduleByCaseUrn_successfulHearingSchedule() {
        final String caseUrn = "CASE123";
        final String hearingId1 = randomUUID().toString();
        final String hearingId2 = randomUUID().toString();
        final String judiciaryId = randomUUID().toString();
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();

        // Create test data
        final Date sittingStart = new Date(System.currentTimeMillis());
        final Date sittingEnd = new Date(System.currentTimeMillis() + 3600000); // 1 hour later

        final CourtSitting courtSitting1 = new CourtSitting(
            sittingStart,
            sittingEnd,
            judiciaryId,
            courtHouseId,
            courtRoomId
        );

        final CourtSitting courtSitting2 = new CourtSitting(
            sittingStart,
            sittingEnd,
            judiciaryId,
            courtHouseId,
            courtRoomId
        );

        final Hearing hearing1 = new Hearing(
            hearingId1,
            "First Hearing",
            "Initial hearing description",
            "Note for first hearing",
            List.of(courtSitting1)
        );

        final Hearing hearing2 = new Hearing(
            hearingId2,
            "Second Hearing",
            "Follow-up hearing description",
            "Note for second hearing",
            List.of(courtSitting2)
        );

        final CourtSchedule courtSchedule = new CourtSchedule(List.of(hearing1, hearing2));
        final CourtScheduleSchema schema = new CourtScheduleSchema(List.of(courtSchedule));
        final ResponseEntity<CourtScheduleSchema> response = ResponseEntity.ok(schema);

        when(courtScheduleClient.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(response);

        List<CourtScheduleDto> result = courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn);

        assertNotNull(result);
        assertEquals(1, result.size());

        CourtScheduleDto scheduleDto = result.get(0);
        assertNotNull(scheduleDto.hearingDtos());
        assertEquals(2, scheduleDto.hearingDtos().size());

        CourtScheduleDto.HearingDto hearingDto1 = scheduleDto.hearingDtos().get(0);
        assertEquals(hearingId1, hearingDto1.hearingId());
        assertEquals("First Hearing", hearingDto1.hearingType());
        assertEquals("Initial hearing description", hearingDto1.hearingDescription());
        assertEquals("Note for first hearing", hearingDto1.listNote());
        assertEquals(1, hearingDto1.courtSittingDtos().size());

        CourtScheduleDto.HearingDto.CourtSittingDto sittingDto1 = hearingDto1.courtSittingDtos().get(0);
        assertNotNull(sittingDto1.sittingStart());
        assertNotNull(sittingDto1.sittingEnd());
        assertEquals(judiciaryId, sittingDto1.judiciaryId());
        assertEquals(courtHouseId, sittingDto1.courtHouse());
        assertEquals(courtRoomId, sittingDto1.courtRoom());

        CourtScheduleDto.HearingDto hearingDto2 = scheduleDto.hearingDtos().get(1);
        assertEquals(hearingId2, hearingDto2.hearingId());
        assertEquals("Second Hearing", hearingDto2.hearingType());
        assertEquals("Follow-up hearing description", hearingDto2.hearingDescription());
        assertEquals("Note for second hearing", hearingDto2.listNote());
        assertEquals(1, hearingDto2.courtSittingDtos().size());
    }

    @Test
    void testGetScheduleByCaseUrn_emptyHearingSchedule() {
        final String caseUrn = "CASE123";
        final CourtSchedule courtSchedule = new CourtSchedule(List.of());
        final CourtScheduleSchema schema = new CourtScheduleSchema(List.of(courtSchedule));
        final ResponseEntity<CourtScheduleSchema> response = ResponseEntity.ok(schema);

        when(courtScheduleClient.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(response);

        List<CourtScheduleDto> result = courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).hearingDtos());
        assertEquals(0, result.get(0).hearingDtos().size());
    }
}
