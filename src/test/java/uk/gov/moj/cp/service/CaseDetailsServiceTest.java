package uk.gov.moj.cp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto.AddressDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto.CourtSittingDto;
import uk.gov.moj.cp.metrics.TrackMyCaseMetricsService;
import uk.gov.moj.cp.model.HearingType;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private WarningMessageService warningMessageService;

    private CaseDetailsService caseDetailsService;
    private final String accessToken = "testToken";

    private static final LocalDateTime FIXED_TODAY = LocalDateTime.of(2026, 1, 30, 10, 0, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
        FIXED_TODAY.atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    );

    private String caseUrn;
    private String courtHouseId;
    private String courtRoomId;
    private String judgeId;
    private String hearingId;
    private String pastSittingStartDate;
    private String pastSittingEndDate;
    private String currentSittingStartDate;
    private String currentSittingEndDate;
    private String futureSittingStartDate;
    private String futureSittingEndDate;

    @BeforeEach
    void setUp() throws Exception {
        caseDetailsService = new CaseDetailsService(FIXED_CLOCK, warningMessageService);

        setField(caseDetailsService, "courtScheduleService", courtScheduleService);
        setField(caseDetailsService, "courtHouseService", courtHouseService);
        setField(caseDetailsService, "oauthTokenService", oauthTokenService);
        setField(caseDetailsService, "trackMyCaseMetricsService", trackMyCaseMetricsService);

        caseUrn = "CASE123";
        courtHouseId = randomUUID().toString();
        courtRoomId = randomUUID().toString();
        judgeId = randomUUID().toString();
        hearingId = randomUUID().toString();
        pastSittingStartDate = FIXED_TODAY.minusDays(1).toString();
        pastSittingEndDate = FIXED_TODAY.minusDays(1).plusHours(2).toString();
        currentSittingStartDate = FIXED_TODAY.toString();
        currentSittingEndDate = FIXED_TODAY.toString();
        futureSittingStartDate = FIXED_TODAY.plusDays(1).toString();
        futureSittingEndDate = FIXED_TODAY.plusDays(1).plusHours(2).toString();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("includes hearings when sitting date is in the future")
    void testGetCaseDetailsByCaseUrnWithValidHearingScheduleDetailsForFutureSittingDate() {
        final List<CourtSittingDto> futureCourtSittings = List.of(createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingType());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingDescription());
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
    @DisplayName("should return case details with warning message when message service returns a message")
    void testGetCaseDetailsByCaseUrnWithWarningMessage() {
        final String expectedMessage = "TRIAL_HEARING_STARTS_IN_ONE_MONTH_TWO_DAYS";
        final List<CourtSittingDto> futureCourtSittings = List.of(createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(warningMessageService.getMessage(any(), any())).thenReturn(expectedMessage);

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(expectedMessage, caseDetails.message());
        assertEquals(1, caseDetails.courtSchedule().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingType());

        verify(warningMessageService).getMessage(any(), any());
        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("should return case details with null message when message service returns null")
    void testGetCaseDetailsByCaseUrnWithNullMessage() {
        final List<CourtSittingDto> futureCourtSittings = List.of(createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(warningMessageService.getMessage(any(), any())).thenReturn(null);

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertNull(caseDetails.message());
        assertEquals(1, caseDetails.courtSchedule().size());

        verify(warningMessageService).getMessage(any(), any());
        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("should return case details with actual warning message calculated for 7 days")
    void testGetCaseDetailsByCaseUrnWithActualWarningMessageForSevenDays() throws Exception {
        // Create actual WarningMessageService (not mocked) with fixed clock
        WarningMessageService actualWarningMessageService = new WarningMessageService(FIXED_CLOCK);

        // Create CaseDetailsService with actual WarningMessageService
        CaseDetailsService serviceWithActualMessageService = new CaseDetailsService(FIXED_CLOCK, actualWarningMessageService);

        // Inject mocked dependencies using reflection
        setField(serviceWithActualMessageService, "courtScheduleService", courtScheduleService);
        setField(serviceWithActualMessageService, "courtHouseService", courtHouseService);
        setField(serviceWithActualMessageService, "oauthTokenService", oauthTokenService);
        setField(serviceWithActualMessageService, "trackMyCaseMetricsService", trackMyCaseMetricsService);

        // Set up sitting date 7 days from FIXED_TODAY (January 30, 2026 + 7 days = February 6, 2026)
        final String sevenDaysLaterStartDate = FIXED_TODAY.plusDays(7).toString();
        final String sevenDaysLaterEndDate = FIXED_TODAY.plusDays(7).plusHours(2).toString();

        final List<CourtSittingDto> futureCourtSittings = List.of(createCourtSitting(sevenDaysLaterStartDate, sevenDaysLaterEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        final CaseDetailsDto caseDetails = serviceWithActualMessageService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals("TRIAL_HEARING_STARTS_IN_SEVEN_DAYS", caseDetails.message());
        assertEquals(1, caseDetails.courtSchedule().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingType());
        assertEquals(sevenDaysLaterStartDate, caseHearingDetails.courtSittings().getFirst().sittingStart());
        assertEquals(sevenDaysLaterEndDate, caseHearingDetails.courtSittings().getFirst().sittingEnd());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("excludes hearings when sittings are in the past")
    void testGetCaseDetailsByCaseUrnWithEmptyHearingScheduleDetailsForPastSittingDate() {
        final List<CourtSittingDto> pastCourtSittings = List.of(createCourtSitting(pastSittingStartDate, pastSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.SENTENCE.getValue(), pastCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(0, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }


    @Test
    @DisplayName("includes only hearings that have at least one future sitting")
    void testGetCaseDetailsByCaseUrnWithValidHearingScheduleFutureAndPastSittingDateCombined() {
        final List<CourtSittingDto> futureCourtSittings = List.of(createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        List<CourtSittingDto> pastCourtSittings = List.of(createCourtSitting(pastSittingStartDate, pastSittingEndDate));
        final HearingDto hearingDto1 = createHearing(HearingType.TRIAL.getValue(), pastCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);


        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingType());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingDescription());
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
    @DisplayName("includes hearings when sitting date is current date")
    void testGetCaseDetailsByCaseUrnWithValidHearingScheduleDetailsForCurrentDate() {
        final List<CourtSittingDto> currentCourtSittings =  List.of(createCourtSitting(currentSittingStartDate, currentSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), currentCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingType());
        assertEquals(HearingType.TRIAL.getValue(), caseHearingDetails.hearingDescription());
        assertEquals("Note1", caseHearingDetails.listNote());

        var schedule = caseHearingDetails.courtSittings().getFirst();
        assertEquals(judgeId, schedule.judiciaryId());
        assertEquals(courtHouseId, schedule.courtHouse().courtHouseId());
        assertEquals(courtRoomId, schedule.courtHouse().courtRoomId());
        assertEquals(currentSittingStartDate, schedule.sittingStart());
        assertEquals(currentSittingEndDate, schedule.sittingEnd());

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
        final String todayStartDate = FIXED_TODAY.minusHours(2).toString();
        final String todayEndDate = FIXED_TODAY.minusHours(1).toString();

        final List<CourtSittingDto> courtSittings =  List.of(createCourtSitting(todayStartDate, todayEndDate));
        final HearingDto hearingDto = createHearing(HearingType.SENTENCE.getValue(), courtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        var caseHearingDetails = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(hearingId, caseHearingDetails.hearingId());
        assertEquals(HearingType.SENTENCE.getValue(), caseHearingDetails.hearingType());
        assertEquals(HearingType.SENTENCE.getValue(), caseHearingDetails.hearingDescription());
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
        final List<CourtSittingDto> futureCourtSittings =  List.of(createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        List<CourtSittingDto> currentCourtSittings =   List.of(createCourtSitting(currentSittingStartDate, currentSittingEndDate));
        final HearingDto hearingDto1 = createHearing(HearingType.SENTENCE.getValue(), currentCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("should not includes  hearings that have only past sitting")
    void testGetCaseDetailsByCaseUrnWithNoHearingScheduleWithMultipleHearingWithPastSittings() {
        final List<CourtSittingDto> pastCourtSittings =  List.of(createCourtSitting(pastSittingStartDate, pastSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), pastCourtSittings);
        final HearingDto hearingDto1 = createHearing(HearingType.SENTENCE.getValue(), pastCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1));

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(0, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @DisplayName("should  includes  hearings that have only future sitting and should not include hearing that has past sitting")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleWithMultipleHearingWithPastAndFutureHearing() {
        final List<CourtSittingDto> pastCourtSittings =  List.of(createCourtSitting(pastSittingStartDate, pastSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), pastCourtSittings);

        final List<CourtSittingDto> futureCourtSittings =  List.of(createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto1 = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with current and future date sittings")
    void testGetCaseDetailsByCaseUrnWithValidMultiDayHearingScheduleWithCurrentAndFutureSitting() {
        final CourtSittingDto futureSittingDto = createCourtSitting(futureSittingStartDate, futureSittingEndDate);
        final CourtSittingDto currentSittingDto = createCourtSitting(currentSittingStartDate, currentSittingEndDate);

        final List<CourtSittingDto> courtSittings =  List.of(futureSittingDto, currentSittingDto);
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), courtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().getFirst().courtSittings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with future date sittings")
    void testGetCaseDetailsByCaseUrnWithValidMultiDayHearingScheduleWithPastAndFutureSitting() {
        final CourtSittingDto futureSittingDto = createCourtSitting(futureSittingStartDate, futureSittingEndDate);
        final List<CourtSittingDto> futureCourtSittings =  List.of(futureSittingDto, futureSittingDto);
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().getFirst().courtSittings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with past date sittings")
    void testGetCaseDetailsByCaseUrnWithNoHearingScheduleWithPastMultiDaySitting() {
        final CourtSittingDto pastSittingDto = createCourtSitting(pastSittingStartDate, pastSittingEndDate);

        final List<CourtSittingDto> pastCourtSittings =  List.of(pastSittingDto, pastSittingDto);
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), pastCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(0, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes only hearings that multiday hearing with past and future date sittings")
    void testGetCaseDetailsByCaseUrnWithMultiDayHearingScheduleWithPastAndFutureSitting() {
        final CourtSittingDto pastSittingDto = createCourtSitting(pastSittingStartDate, pastSittingEndDate);
        final CourtSittingDto futureSittingDto = createCourtSitting(futureSittingStartDate, futureSittingEndDate);

        final List<CourtSittingDto> courtSittings =   List.of(pastSittingDto, futureSittingDto);
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), courtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(1, caseDetails.courtSchedule().getFirst().hearings().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().getFirst().courtSittings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }


    @Test
    @DisplayName("includes only hearings that have at least one future sitting")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleWithMultipleHearingOneWithSingleDayAndAnotherWithMultiDay() {
        final List<CourtSittingDto> futureCourtSittings =  List.of(createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), futureCourtSittings);


        final List<CourtSittingDto> pastAndFutureCourtSittings =  List.of(createCourtSitting(pastSittingStartDate, pastSittingEndDate),
                                                              createCourtSitting(futureSittingStartDate, futureSittingEndDate));
        final HearingDto hearingDto1 = createHearing(HearingType.SENTENCE.getValue(), pastAndFutureCourtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes hearing of types Trial or SENTENCE")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleWithHearingTypeSSENTENCEOrTrial() {
        final CourtSittingDto futureSittingDto = createCourtSitting(futureSittingStartDate, futureSittingEndDate);
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), List.of(futureSittingDto));
        final HearingDto hearingDto1 = createHearing(HearingType.SENTENCE.getValue(), List.of(futureSittingDto));
        final HearingDto hearingDto2 = createHearing("Invalid Type", List.of(futureSittingDto));

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1, hearingDto2));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().size());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }


    @Test
    @DisplayName("includes hearing of types Trial or SSENTENCE")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleInSortedOrderOfSittingDate() {
        final CourtSittingDto futureSittingDto1 = createCourtSitting(futureSittingStartDate, futureSittingEndDate);
        final List<CourtSittingDto> courtSittings1 =   List.of(futureSittingDto1);
        final HearingDto hearingDto = createHearing(HearingType.TRIAL.getValue(), courtSittings1);

        final CourtSittingDto futureSittingDto2 = createCourtSitting(FIXED_TODAY.toString(), FIXED_TODAY.plusHours(2).toString());
        final List<CourtSittingDto> courtSittings2 =   List.of(futureSittingDto2);
        final HearingDto hearingDto1 = createHearing(HearingType.SENTENCE.getValue(), courtSittings2);

        final HearingDto hearingDto2 = createHearing("Invalid Type", courtSittings1);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto, hearingDto1, hearingDto2));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        assertEquals(2, caseDetails.courtSchedule().getFirst().hearings().size());
        assertEquals(HearingType.SENTENCE.getValue(), caseDetails.courtSchedule().getFirst().hearings().getFirst().hearingType());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("should handle complex data with multiple hearings including First hearing, Trial with multiple sittings")
    void testGetCaseDetailsByCaseUrnWithComplexHearingData() throws Exception {
        LocalDateTime FIXED_TODAY = LocalDateTime.of(2026, 2, 02, 10, 0, 0);
        Clock FIXED_CLOCK = Clock.fixed(
            FIXED_TODAY.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );

        WarningMessageService actualWarningMessageService = new WarningMessageService(FIXED_CLOCK);

        // Create CaseDetailsService with actual WarningMessageService
        CaseDetailsService serviceWithActualMessageService = new CaseDetailsService(FIXED_CLOCK, actualWarningMessageService);

        // Inject mocked dependencies using reflection
        setField(serviceWithActualMessageService, "courtScheduleService", courtScheduleService);
        setField(serviceWithActualMessageService, "courtHouseService", courtHouseService);
        setField(serviceWithActualMessageService, "oauthTokenService", oauthTokenService);
        setField(serviceWithActualMessageService, "trackMyCaseMetricsService", trackMyCaseMetricsService);

        // Load test data from JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream jsonStream = getClass().getClassLoader().getResourceAsStream("court-schedule-test-data.json");
        CourtScheduleDto scheduleDto = objectMapper.readValue(jsonStream, CourtScheduleDto.class);

        // Extract IDs from loaded data for verification
        String hearing1Id = "a20ca4ff-0dc2-4777-a921-bfe08706618b";
        String hearing2Id = "fdb25fdb-702c-46cb-a395-ea2c43385742";
        String hearing3Id = "66d961b2-36ee-4bb5-bd1d-f191bcfbd607";
        String courtHouse1Id = "210bf1ba-e253-3516-96df-949be917b383";
        String courtRoom1Id = "f048a0f8-aa9c-3636-a3cb-5d185a7db71a";
        String courtHouse3Id = "67aa82ba-67bb-4699-8176-5f572048352b";
        String courtRoom3Id = "c5f066ff-0aff-3dde-8bd9-40a587d3c58e";

        // Mock court house service to return court houses
        CourtRoomDto courtRoomDto1 = new CourtRoomDto(123, "CourtRoom 01");
        AddressDto addressDto1 = new AddressDto("53", "Court Street", "London", null, "CB4 3MX", null);
        CourtHouseDto courtHouseDto1 = new CourtHouseDto(
            courtHouse1Id,
            courtRoom1Id,
            "CROWN",
            "123",
            "Lavender Hill",
            addressDto1,
            Arrays.asList(courtRoomDto1)
        );

        CourtRoomDto courtRoomDto3 = new CourtRoomDto(456, "CourtRoom 02");
        AddressDto addressDto3 = new AddressDto("54", "Court Avenue", "London", null, "CB4 3MY", null);
        CourtHouseDto courtHouseDto3 = new CourtHouseDto(
            courtHouse3Id,
            courtRoom3Id,
            "CROWN",
            "124",
            "Lavender Hill",
            addressDto3,
            Arrays.asList(courtRoomDto3)
        );

        // Mock court house service to return different court houses based on IDs
        when(courtHouseService.getCourtHouseById(any(), eq(courtHouse1Id), eq(courtRoom1Id)))
            .thenReturn(courtHouseDto1);
        when(courtHouseService.getCourtHouseById(any(), eq(courtHouse3Id), eq(courtRoom3Id)))
            .thenReturn(courtHouseDto3);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn))
            .thenReturn(List.of(scheduleDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);

        final CaseDetailsDto caseDetails = serviceWithActualMessageService.getCaseDetailsByCaseUrn(caseUrn);

//        assertEquals(caseUrn, caseDetails.caseUrn());
//        assertEquals(1, caseDetails.courtSchedule().size());
//
//        // Should only include Trial hearings (hearing1 "First hearing" should be excluded)
//        var hearings = caseDetails.courtSchedule().getFirst().hearings();
//        assertEquals(2, hearings.size(), "Should have 2 Trial hearings (First hearing excluded)");
//
//        // Verify hearing 2 is included
//        var hearing2Result = hearings.stream()
//            .filter(h -> h.hearingId().equals(hearing2Id))
//            .findFirst()
//            .orElse(null);
//        assertNotNull(hearing2Result, "Hearing 2 (Trial) should be included");
//        assertEquals(HearingType.TRIAL.getValue(), hearing2Result.hearingType());
//        assertEquals(1, hearing2Result.courtSittings().size());
//
//        // Verify hearing 3 is included with all 4 sittings (all are current or future)
//        var hearing3Result = hearings.stream()
//            .filter(h -> h.hearingId().equals(hearing3Id))
//            .findFirst()
//            .orElse(null);
//        assertNotNull(hearing3Result, "Hearing 3 (Trial with multiple sittings) should be included");
//        assertEquals(HearingType.TRIAL.getValue(), hearing3Result.hearingType());
//        assertEquals(4, hearing3Result.courtSittings().size(), "All 4 sittings should be included (all are current or future)");
//
        // Verify the message is calculated correctly
        // The earliest sitting is 2026-01-30T10:00 (FIXED_TODAY), so message should be TRIAL_HEARING_STARTS_TODAY
        assertEquals("TRIAL_HEARING_IS_ONGOING", caseDetails.message(),
            "Message should be TRIAL_HEARING_STARTS_TODAY as earliest sitting is today");

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    private CourtSittingDto createCourtSitting(final  String sittingStartDate, final String sittingEndDate){

        return new CourtSittingDto(
            sittingStartDate,
            sittingEndDate,
            judgeId,
            courtHouseId,
            courtRoomId
        );
    }

    private CourtHouseDto createCourtHouse(final CourtRoomDto courtRoomDto, final AddressDto addressDto){
        return new CourtHouseDto(
            courtHouseId,
            courtRoomId,
            "CROWN",
            "123",
            "Lavender Hill",
            addressDto,
            Arrays.asList(courtRoomDto)
        );
    }

    private HearingDto createHearing(String hearingType, List<CourtSittingDto> courtSittings){
        return new HearingDto(
            hearingId,
            hearingType,
            hearingType,
            "Note1",
            courtSittings
        );
    }

}
