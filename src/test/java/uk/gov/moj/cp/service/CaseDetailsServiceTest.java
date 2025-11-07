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
    @Mock
    private TokenService tokenService;


    @InjectMocks
    private CaseDetailsService caseDetailsService;
    private final String token = "testToken";


    @Test
    @DisplayName("includes hearings when sitting date is in the future")
    void testGetCaseDetailsByCaseUrnWithValidHearingScheduleDetailsForFutureSittingDate() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String sittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String sittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();

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

        when(courtScheduleService.getCourtScheduleByCaseUrn(token, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);
        when(tokenService.getJwtToken()).thenReturn(token);

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


    @Test
    @DisplayName("excludes hearings when  sittings are  in the past")
    void testGetCaseDetailsByCaseUrnWithEmptyHearingScheduleDetailsForPastSittingDate() {
        final String caseUrn = "CASE123";
        final String sittingStartDate = LocalDateTime.now().minusDays(1).toString();
        final String sittingEndDate = LocalDateTime.now().minusDays(1).plusHours(2).toString();

        final CourtSittingDto sittingDto = new CourtSittingDto(
                sittingStartDate,
                sittingEndDate,
                randomUUID().toString(),
                randomUUID().toString(),
                randomUUID().toString()
        );

        final HearingDto hearingDto = new HearingDto(
                randomUUID().toString(),
                "First Hearing",
                "First Hearing",
                "Note1",
                List.of(sittingDto)
        );

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));
        when(tokenService.getJwtToken()).thenReturn(token);

        when(courtScheduleService.getCourtScheduleByCaseUrn(token, caseUrn)).thenReturn(List.of(scheduleDto));

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(0, caseDetails.courtSchedule().get(0).hearings().size());
    }


    @Test
    @DisplayName("includes only hearings that have at least one future sitting")
    void testGetCaseDetailsByCaseUrnWithValidHearingScheduleFutureAndPastSittingDateCombined() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();
        final String pastSittingStartDate = LocalDateTime.now().minusDays(1).toString();
        final String pastSittingEndDate = LocalDateTime.now().minusDays(1).plusHours(2).toString();

        final CourtSittingDto futureSittingDto = new CourtSittingDto(
                futureSittingStartDate,
                futureSittingEndDate,
                judgeId,
                courtHouseId,
                courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
                hearingId,
                "First Hearing",
                "First Hearing",
                "Note1",
                List.of(futureSittingDto)
        );

        final CourtSittingDto pastSittingDto = new CourtSittingDto(
                pastSittingStartDate,
                pastSittingEndDate,
                judgeId,
                courtHouseId,
                courtRoomId
        );
        final HearingDto hearingDto1 = new HearingDto(
                hearingId,
                "First Hearing",
                "First Hearing",
                "Note1",
                List.of(pastSittingDto)
        );


        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1));

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

        when(tokenService.getJwtToken()).thenReturn(token);
        when(courtScheduleService.getCourtScheduleByCaseUrn(token, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().get(0).hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().get(0).hearings().get(0);
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().get(0);
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(futureSittingStartDate, schedule.sittingStart());
        assertEquals(futureSittingEndDate, schedule.sittingEnd());

        assertEquals("53", schedule.courtHouse().address().address1());
        assertEquals("Court Street", schedule.courtHouse().address().address2());
        assertEquals("London", schedule.courtHouse().address().address3());

        assertEquals(123, schedule.courtHouse().courtRoomDtoList().get(0).courtRoomId());
        assertEquals("CourtRoom 01", schedule.courtHouse().courtRoomDtoList().get(0).courtRoomName());
    }


    @Test
    @DisplayName("includes hearings when sitting date is today")
    void testGetCaseDetailsByCaseUrnWithValidMultipleHearingScheduleForFutureSittingDate() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String todayStartDate = LocalDateTime.now().toString();
        final String todayEndDate = LocalDateTime.now().plusHours(2).toString();


        final CourtSittingDto todaySittingDto = new CourtSittingDto(
                todayStartDate,
                todayEndDate,
                judgeId,
                courtHouseId,
                courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
                hearingId,
                "First Hearing",
                "First Hearing",
                "Note1",
                List.of(todaySittingDto)
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
        when(tokenService.getJwtToken()).thenReturn(token);

        when(courtScheduleService.getCourtScheduleByCaseUrn(token, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().get(0).hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().get(0).hearings().get(0);
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().get(0);
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(todayStartDate, schedule.sittingStart());
        assertEquals(todayEndDate, schedule.sittingEnd());

        assertEquals("53", schedule.courtHouse().address().address1());
        assertEquals("Court Street", schedule.courtHouse().address().address2());
        assertEquals("London", schedule.courtHouse().address().address3());

        assertEquals(123, schedule.courtHouse().courtRoomDtoList().get(0).courtRoomId());
        assertEquals("CourtRoom 01", schedule.courtHouse().courtRoomDtoList().get(0).courtRoomName());
    }

    @Test
    @DisplayName("includes hearings when sitting date is today, but before now")
    void testGetCaseDetailsByCaseUrnWithValidMultipleHearingScheduleForFutureSittingDateBeforeNow() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String todayStartDate = LocalDateTime.now().minusHours(2).toString();
        final String todayEndDate = LocalDateTime.now().minusHours(1).toString();


        final CourtSittingDto todaySittingDto = new CourtSittingDto(
                todayStartDate,
                todayEndDate,
                judgeId,
                courtHouseId,
                courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
                hearingId,
                "First Hearing",
                "First Hearing",
                "Note1",
                List.of(todaySittingDto)
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
        when(tokenService.getJwtToken()).thenReturn(token);

        when(courtScheduleService.getCourtScheduleByCaseUrn(token, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().get(0).hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().get(0).hearings().get(0);
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().get(0);
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(todayStartDate, schedule.sittingStart());
        assertEquals(todayEndDate, schedule.sittingEnd());

        assertEquals("53", schedule.courtHouse().address().address1());
        assertEquals("Court Street", schedule.courtHouse().address().address2());
        assertEquals("London", schedule.courtHouse().address().address3());

        assertEquals(123, schedule.courtHouse().courtRoomDtoList().get(0).courtRoomId());
        assertEquals("CourtRoom 01", schedule.courtHouse().courtRoomDtoList().get(0).courtRoomName());
    }
}
