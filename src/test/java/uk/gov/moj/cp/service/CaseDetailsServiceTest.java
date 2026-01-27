package uk.gov.moj.cp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import uk.gov.moj.cp.metrics.TrackMyCaseMetricsService;

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
    private OAuthTokenService oauthTokenService;

    @Mock
    private TrackMyCaseMetricsService trackMyCaseMetricsService;

    @InjectMocks
    private CaseDetailsService caseDetailsService;
    private final String accessToken = "testToken";


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

        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().getFirst();
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(sittingStartDate, schedule.sittingStart());
        assertEquals(sittingEndDate, schedule.sittingEnd());

        assertEquals("53", schedule.courtHouse().address().address1());
        assertEquals("Court Street", schedule.courtHouse().address().address2());
        assertEquals("London", schedule.courtHouse().address().address3());

        assertEquals(123, schedule.courtHouse().courtRoomDtoList().getFirst().courtRoomId());
        assertEquals("CourtRoom 01", schedule.courtHouse().courtRoomDtoList().getFirst().courtRoomName());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
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
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(0, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().getFirst();
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(futureSittingStartDate, schedule.sittingStart());
        assertEquals(futureSittingEndDate, schedule.sittingEnd());

        assertEquals("53", schedule.courtHouse().address().address1());
        assertEquals("Court Street", schedule.courtHouse().address().address2());
        assertEquals("London", schedule.courtHouse().address().address3());

        assertEquals(123, schedule.courtHouse().courtRoomDtoList().getFirst().courtRoomId());
        assertEquals("CourtRoom 01", schedule.courtHouse().courtRoomDtoList().getFirst().courtRoomName());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }


    @Test
    @DisplayName("includes hearings when sitting date is today")
    void testGetCaseDetailsByCaseUrnWithValidHearingScheduleDetailsForCurrentDate()
    {
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
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().getFirst();
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(todayStartDate, schedule.sittingStart());
        assertEquals(todayEndDate, schedule.sittingEnd());

        assertEquals("53", schedule.courtHouse().address().address1());
        assertEquals("Court Street", schedule.courtHouse().address().address2());
        assertEquals("London", schedule.courtHouse().address().address3());

        assertEquals(123, schedule.courtHouse().courtRoomDtoList().getFirst().courtRoomId());
        assertEquals("CourtRoom 01", schedule.courtHouse().courtRoomDtoList().getFirst().courtRoomName());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes hearings when sitting date is today, but before now")
    void testGetCaseDetailsByCaseUrnWithValidHearingScheduleDetailsForCurrentDateButBeforeCurrentTime() {
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
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals("First Hearing", caseHearingDetails.hearingType());
        assertEquals("First Hearing", caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().getFirst();
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

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that have at least one future sitting")
    void testGetCaseDetailsByCaseUrnWithValidMultipleHearingSchedule() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();
        final String currentSittingStartDate = LocalDateTime.now().toString();
        final String currentSittingEndDate = LocalDateTime.now().plusHours(2).toString();

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

        final CourtSittingDto currentSittingDto = new CourtSittingDto(
            currentSittingStartDate,
            currentSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );
        final HearingDto hearingDto1 = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(currentSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("should not includes  hearings that have only past sitting")
    void testGetCaseDetailsByCaseUrnWithNoHearingScheduleWithMultipleHearingWithPastSittings() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String pastSittingStartDate = LocalDateTime.now().minusDays(1).toString();
        final String pastSittingEndDate = LocalDateTime.now().minusDays(1).plusHours(2).toString();

        final CourtSittingDto pastSittingDto = new CourtSittingDto(
            pastSittingStartDate,
            pastSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(pastSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        //when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(0, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @DisplayName("should  includes  hearings that have only future sitting and should not include hearing that has past sitting")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleWithMultipleHearingWithPastAndFutureHearing() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String pastSittingStartDate = LocalDateTime.now().minusDays(1).toString();
        final String pastSittingEndDate = LocalDateTime.now().minusDays(1).plusHours(2).toString();

        final String futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();

        final CourtSittingDto pastSittingDto = new CourtSittingDto(
            pastSittingStartDate,
            pastSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(pastSittingDto)
        );

        final CourtSittingDto futureSittingDto = new CourtSittingDto(
            futureSittingStartDate,
            futureSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

        final HearingDto hearingDto1 = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(futureSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with current and future date sittings")
    void testGetCaseDetailsByCaseUrnWithValidMultiDayHearingScheduleWithCurrentAndFutureSitting() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();
        final String currentSittingStartDate = LocalDateTime.now().toString();
        final String currentSittingEndDate = LocalDateTime.now().plusHours(2).toString();

        final CourtSittingDto futureSittingDto = new CourtSittingDto(
            futureSittingStartDate,
            futureSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

        final CourtSittingDto currentSittingDto = new CourtSittingDto(
            currentSittingStartDate,
            currentSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(futureSittingDto, currentSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().getFirst().courtSittings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with future date sittings")
    void testGetCaseDetailsByCaseUrnWithValidMultiDayHearingScheduleWithPastAndFutureSitting() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();

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
            List.of(futureSittingDto, futureSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().getFirst().courtSittings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with past date sittings")
    void testGetCaseDetailsByCaseUrnWithNoHearingScheduleWithPastMultiDaySitting() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String pastSittingStartDate = LocalDateTime.now().minusDays(1).toString();
        final String pastSittingEndDate = LocalDateTime.now().minusDays(1).plusHours(2).toString();


        final CourtSittingDto pastSittingDto = new CourtSittingDto(
            pastSittingStartDate,
            pastSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

        final HearingDto hearingDto = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(pastSittingDto, pastSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(0, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with past and future date sittings")
    void testGetCaseDetailsByCaseUrnWithMultiDayHearingScheduleWithPastAndFutureSitting() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String pastSittingStartDate = LocalDateTime.now().minusDays(1).toString();
        final String pastSittingEndDate = LocalDateTime.now().minusDays(1).plusHours(2).toString();
        final String futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();


        final CourtSittingDto pastSittingDto = new CourtSittingDto(
            pastSittingStartDate,
            pastSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );

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
            List.of(pastSittingDto, futureSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().getFirst().courtSittings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }


    @Test
    @DisplayName("includes only hearings that have at least one future sitting")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleWithMultipleHearingOneWithSingleDayAndAnotherWithMultiDay() {
        final String caseUrn = "CASE123";
        final String courtHouseId = randomUUID().toString();
        final String courtRoomId = randomUUID().toString();
        final String judgeId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        final String futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();
        final String currentSittingStartDate = LocalDateTime.now().toString();
        final String currentSittingEndDate = LocalDateTime.now().plusHours(2).toString();

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
            currentSittingStartDate,
            currentSittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );
        final HearingDto hearingDto1 = new HearingDto(
            hearingId,
            "First Hearing",
            "First Hearing",
            "Note1",
            List.of(pastSittingDto, futureSittingDto)
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

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(courtHouseDto);

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }
}
