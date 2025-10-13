package uk.gov.moj.cp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static java.util.UUID.randomUUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto.AddressDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto.CourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CaseDetailsServiceTest {

    @Mock
    private CourtScheduleService courtScheduleService;

    @Mock
    private CourtHouseService courtHouseService;

    @InjectMocks
    private CaseDetailsService caseDetailsService;

    @Test
    @DisplayName("getCaseDetailsByCaseUrn returns correct CaseDetailsDto")
    void testGetCaseDetailsByCaseDetailsByCaseUrn() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String sittingStartDate = LocalDateTime.now().toString();
        final String sittingEndDate = LocalDateTime.now().plusHours(2).toString();

        final CourtSittingDto sittingDto = new CourtSittingDto(
            sittingStartDate,
            sittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(sittingDto)
        );

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        final CourtHouseDto courtHouseDto = new CourtHouseDto(
            courtHouseId,
            courtRoomId,
            "CROWN",
            "123",
            "Lavender Hill",
            addressDto,
            Arrays.asList(courtRoomDto)
        );

        when(courtScheduleService.getCourtScheduleByCaseUrn(caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());

        var caseHearingDetails = caseDetails.courtSchedule().get(0).hearings().get(0);
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().get(0);
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(sittingStartDate, schedule.sittingStart());
        assertEquals(sittingEndDate, schedule.sittingEnd());

        assertEquals("53", schedule.courtHouse().address().address1());
        assertEquals("Court Street", schedule.courtHouse().address().address2());
        assertEquals("London", schedule.courtHouse().address().address3());

        assertEquals(123, schedule.courtHouse().courtRoomDtoList().get(0).courtRoomId());
        assertEquals("CourtRoom 01", schedule.courtHouse().courtRoomDtoList().get(0).courtRoomName());
    }
}
