package uk.gov.moj.cp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.UUID.randomUUID;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto.AddressDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto.CourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto;
import uk.gov.moj.cp.metrics.TrackMyCaseMetricsService;
import uk.gov.moj.cp.model.HearingType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private String datePlus7 ;
    private String datePlus13 ;
    private String datePlus14  ;
    private String datePlus21  ;
    private String datePlus42  ;
    private String datePlus49  ;
    private String datePlus43  ;
    private String datePlus50  ;


    @BeforeEach
    void setUp(){
        caseUrn = "CASE123";
        courtHouseId = randomUUID().toString();
        courtRoomId = randomUUID().toString();
        judgeId = randomUUID().toString();
        hearingId = randomUUID().toString();
        pastSittingStartDate = LocalDateTime.now().minusDays(1).toString();
        pastSittingEndDate = LocalDateTime.now().minusDays(1).plusHours(2).toString();
        currentSittingStartDate = LocalDateTime.now().toString();
        currentSittingEndDate = LocalDateTime.now().toString();
        futureSittingStartDate = LocalDateTime.now().plusDays(1).toString();
        futureSittingEndDate = LocalDateTime.now().plusDays(1).plusHours(2).toString();

        datePlus7 = LocalDate.now().plusDays(7).toString();
        datePlus13 = LocalDate.now().plusDays(13).toString();
        datePlus14 = LocalDate.now().plusDays(14).toString();
        datePlus21 = LocalDate.now().plusDays(21).toString();
        datePlus42 = LocalDate.now().plusDays(42).toString();
        datePlus49 = LocalDate.now().plusDays(49).toString();
        datePlus43 = LocalDate.now().plusDays(43).toString();
        datePlus50 = LocalDate.now().plusDays(50).toString();

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
        final String todayStartDate = LocalDateTime.now().minusHours(2).toString();
        final String todayEndDate = LocalDateTime.now().minusHours(1).toString();

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

        final CourtSittingDto futureSittingDto2 = createCourtSitting(LocalDateTime.now().toString(), LocalDateTime.now().plusHours(2).toString());
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
    @DisplayName("includes hearing of types Trial or SENTENCE")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleInSortedOrderOfSittingDateAndHearingType() {
        final CourtSittingDto currentSittingDto = createCourtSitting(currentSittingStartDate, currentSittingEndDate);
        final List<CourtSittingDto> courtSittings =   List.of(currentSittingDto);

        final HearingDto hearingDto1 = createHearing("1", HearingType.SENTENCE.getValue(), courtSittings);
        final HearingDto hearingDto2  = createHearing("2", HearingType.SENTENCE.getValue(), courtSittings);
        final HearingDto hearingDto3 = createHearing("3", HearingType.TRIAL.getValue(), courtSittings);
        final HearingDto hearingDto4 = createHearing("4", HearingType.TRIAL.getValue(), courtSittings);
        final HearingDto hearingDto5 = createHearing("Invalid Type", courtSittings);

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto1, hearingDto3, hearingDto2, hearingDto4, hearingDto5));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        List<CaseDetailsHearingDto> hearings = caseDetails.courtSchedule().getFirst().hearings();
        assertEquals(4, hearings.size());

        assertEquals(HearingType.TRIAL.getValue(), hearings.get(0).hearingType());
        assertEquals("3", hearings.get(0).hearingId());

        assertEquals(HearingType.TRIAL.getValue(), hearings.get(1).hearingType());
        assertEquals("4", hearings.get(1).hearingId());

        assertEquals(HearingType.SENTENCE.getValue(), hearings.get(2).hearingType());
        assertEquals("1", hearings.get(2).hearingId());

        assertEquals(HearingType.SENTENCE.getValue(), hearings.get(3).hearingType());
        assertEquals("2", hearings.get(3).hearingId());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes hearing with weekCommencing dates sorted by sitting date")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleInSortedOrderOfSittingDateWithWeeks() {
        final CourtSittingDto futureSittingDto1 = createCourtSitting(null, null);
        final List<CourtSittingDto> courtSittings1 = List.of(futureSittingDto1);


        final HearingDto hearingDto = createHearingWithWeeks(
            "0",
            HearingType.TRIAL.getValue(),
            datePlus7,
            datePlus13,
            "1",
            courtSittings1
        );

        final CourtSittingDto futureSittingDto2 = createCourtSitting(
            null,
            null
        );
        final List<CourtSittingDto> courtSittings2 = List.of(futureSittingDto2);
        final HearingDto hearingDto1 = createHearingWithWeeks(
            "1",
            HearingType.SENTENCE.getValue(),
            datePlus21,
            datePlus21,
            "1",
            courtSittings2
        );

        final CourtSittingDto futureSittingDto3 = createCourtSitting(
            futureSittingStartDate, futureSittingEndDate
        );
        final HearingDto hearingDto2 = createHearingWithWeeks(
            "2",
            "Invalid Type",
            datePlus42,
            datePlus49,
            "1",
            List.of(futureSittingDto3)
        );

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto2, hearingDto1, hearingDto));

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
        assertEquals(HearingType.TRIAL.getValue(), caseDetails.courtSchedule().getFirst().hearings().getFirst().hearingType());

        // Verify weekCommencing fields are set
        final CaseDetailsHearingDto firstHearing = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(datePlus7 , firstHearing.weekCommencingStartDate());
        assertEquals(datePlus13, firstHearing.weekCommencingEndDate());
        assertEquals("1", firstHearing.weekCommencingDurationInWeeks());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    @DisplayName("includes hearing with weekCommencing dates sorted by sitting date")
    void testGetCaseDetailsByCaseUrnWhenCourtSittingAndWeeCommencingDateAreSame() {
        futureSittingStartDate = LocalDateTime.now().plusDays(7).toString();
        futureSittingEndDate = LocalDateTime.now().plusDays(7).plusHours(2).toString();
        final CourtSittingDto futureSittingDto1 = createCourtSitting(futureSittingStartDate, futureSittingEndDate);
        final List<CourtSittingDto> courtSittings1 = List.of(futureSittingDto1);


        final HearingDto hearingDto = createHearingWithWeeks(
            "0",
            HearingType.TRIAL.getValue(),
            datePlus7,
            datePlus13,
            "1",
            courtSittings1
        );

        final CourtSittingDto futureSittingDto2 = createCourtSitting(
            null,
            null
        );
        final List<CourtSittingDto> courtSittings2 = List.of(futureSittingDto2);
        final HearingDto hearingDto1 = createHearingWithWeeks(
            "1",
            HearingType.SENTENCE.getValue(),
            datePlus21,
            datePlus21,
            "1",
            courtSittings2
        );

        final CourtSittingDto futureSittingDto3 = createCourtSitting(
            futureSittingStartDate, futureSittingEndDate
        );
        final HearingDto hearingDto2 = createHearingWithWeeks(
            "2",
            "Invalid Type",
            datePlus42,
            datePlus49,
            "1",
            List.of(futureSittingDto3)
        );

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto2, hearingDto1, hearingDto));

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
        assertEquals(HearingType.TRIAL.getValue(), caseDetails.courtSchedule().getFirst().hearings().getFirst().hearingType());

        // Verify weekCommencing fields are set
        final CaseDetailsHearingDto firstHearing = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals("0" , firstHearing.hearingId());
        assertEquals(datePlus7 , firstHearing.weekCommencingStartDate());
        assertEquals(datePlus13, firstHearing.weekCommencingEndDate());
        assertEquals("1", firstHearing.weekCommencingDurationInWeeks());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }



    @Test
    @DisplayName("includes hearing of types Trial or SENTENCE with weekCommencing dates sorted by sitting date and hearing type")
    void testGetCaseDetailsByCaseUrnWithHearingScheduleInSortedOrderOfSittingDateAndHearingTypeWithWeeks() {
        final CourtSittingDto currentSittingDto = createCourtSitting(
            currentSittingStartDate, currentSittingEndDate);
        final List<CourtSittingDto> courtSittings = List.of(currentSittingDto);

        final HearingDto hearingDto1 = createHearingWithWeeks(
            "1",
            HearingType.SENTENCE.getValue(),
            datePlus7,
            datePlus14,
            "1",
            courtSittings
        );
        final HearingDto hearingDto2 = createHearingWithWeeks(
            "2",
            HearingType.SENTENCE.getValue(),
            datePlus21,
            datePlus21,
            "1",
            courtSittings
        );
        final HearingDto hearingDto3 = createHearingWithWeeks(
            "3",
            HearingType.TRIAL.getValue(),
            datePlus42,
            datePlus49,
            "1",
            courtSittings
        );
        final HearingDto hearingDto4 = createHearingWithWeeks(
            "4",
            HearingType.TRIAL.getValue(),
            datePlus43,
            datePlus50,
            "1",
            courtSittings
        );
        final HearingDto hearingDto5 = createHearingWithWeeks(
            hearingId,
            "Invalid Type",
            datePlus43,
            datePlus50,
            "1",
            courtSittings
        );

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto1, hearingDto3, hearingDto2, hearingDto4, hearingDto5));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(courtHouseService.getCourtHouseById(any(), any(), any())).thenReturn(createCourtHouse(courtRoomDto, addressDto));
        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        List<CaseDetailsHearingDto> hearings = caseDetails.courtSchedule().getFirst().hearings();
        assertEquals(4, hearings.size());

        assertEquals(HearingType.TRIAL.getValue(), hearings.get(0).hearingType());
        assertEquals("3", hearings.get(0).hearingId());
        assertEquals(datePlus42, hearings.get(0).weekCommencingStartDate());
        assertEquals(datePlus49, hearings.get(0).weekCommencingEndDate());
        assertEquals("1", hearings.get(0).weekCommencingDurationInWeeks());

        assertEquals(HearingType.TRIAL.getValue(), hearings.get(1).hearingType());
        assertEquals("4", hearings.get(1).hearingId());
        assertEquals(datePlus43, hearings.get(1).weekCommencingStartDate());
        assertEquals(datePlus50, hearings.get(1).weekCommencingEndDate());
        assertEquals("1", hearings.get(1).weekCommencingDurationInWeeks());

        assertEquals(HearingType.SENTENCE.getValue(), hearings.get(2).hearingType());
        assertEquals("1", hearings.get(2).hearingId());
        assertEquals(datePlus7, hearings.get(2).weekCommencingStartDate());
        assertEquals(datePlus14, hearings.get(2).weekCommencingEndDate());
        assertEquals("1", hearings.get(2).weekCommencingDurationInWeeks());

        assertEquals(HearingType.SENTENCE.getValue(), hearings.get(3).hearingType());
        assertEquals("2", hearings.get(3).hearingId());
        assertEquals(datePlus21, hearings.get(3).weekCommencingStartDate());
        assertEquals(datePlus21, hearings.get(3).weekCommencingEndDate());
        assertEquals("1", hearings.get(3).weekCommencingDurationInWeeks());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }


    @Test
    @DisplayName("includes hearing with weekCommencing dates sorted by sitting date")
    void testGetCaseDetailsByCaseUrnWhenValidurtSittingAndWeekDateIsNull() {
        final CourtSittingDto futureSittingDto1 = createCourtSitting(null, null);
        final List<CourtSittingDto> courtSittings1 = List.of(futureSittingDto1);


        final HearingDto hearingDto = createHearingWithWeeks(
            "0",
            HearingType.TRIAL.getValue(),
            Strings.EMPTY,
            Strings.EMPTY,
            "1",
            courtSittings1
        );

        final CourtSittingDto futureSittingDto2 = createCourtSitting(
            null,
            null
        );
        final List<CourtSittingDto> courtSittings2 = List.of(futureSittingDto2);
        final HearingDto hearingDto1 = createHearingWithWeeks(
            "1",
            HearingType.SENTENCE.getValue(),
            Strings.EMPTY,
            Strings.EMPTY,
            "1",
            courtSittings2
        );

        final CourtSittingDto futureSittingDto3 = createCourtSitting(
            futureSittingStartDate, futureSittingEndDate
        );
        final HearingDto hearingDto2 = createHearingWithWeeks(
            "2",
            HearingType.TRIAL.getValue(),
            Strings.EMPTY,
            Strings.EMPTY,
            "1",
            List.of(futureSittingDto3)
        );

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto2, hearingDto1, hearingDto));

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
        assertEquals(HearingType.TRIAL.getValue(), caseDetails.courtSchedule().getFirst().hearings().getFirst().hearingType());

        // Verify weekCommencing fields are set
        final CaseDetailsHearingDto firstHearing = caseDetails.courtSchedule().getFirst().hearings().getFirst();
        assertEquals(Strings.EMPTY , firstHearing.weekCommencingStartDate());
        assertEquals(Strings.EMPTY, firstHearing.weekCommencingEndDate());

        verify(trackMyCaseMetricsService).incrementCaseDetailsCount(caseUrn);
    }

    @Test
    void testGetCaseDetailsByCaseUrnWhenValidButEmptySittingAndWeekDateIsValid() {
        final CourtSittingDto currentSittingDto = createCourtSitting(
            currentSittingStartDate, currentSittingEndDate);
        final List<CourtSittingDto> courtSittings = List.of();

        final HearingDto hearingDto1 = createHearingWithWeeks(
            "1",
            HearingType.SENTENCE.getValue(),
            datePlus7,
            datePlus14,
            "1",
            courtSittings
        );
        final HearingDto hearingDto2 = createHearingWithWeeks(
            "2",
            HearingType.SENTENCE.getValue(),
            datePlus21,
            datePlus21,
            "1",
            courtSittings
        );
        final HearingDto hearingDto3 = createHearingWithWeeks(
            "3",
            HearingType.TRIAL.getValue(),
            datePlus42,
            datePlus49,
            "1",
            courtSittings
        );
        final HearingDto hearingDto4 = createHearingWithWeeks(
            "4",
            HearingType.TRIAL.getValue(),
            datePlus43,
            datePlus50,
            "1",
            courtSittings
        );

        final CourtScheduleDto scheduleDto = new CourtScheduleDto(List.of(hearingDto3, hearingDto1, hearingDto2, hearingDto4));

        final CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        final AddressDto addressDto = new AddressDto("53", "Court Street",
                                                     "London", null, "CB4 3MX", null);

        when(oauthTokenService.getJwtToken()).thenReturn(accessToken);
        when(courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn)).thenReturn(List.of(scheduleDto));

        final CaseDetailsDto caseDetails = caseDetailsService.getCaseDetailsByCaseUrn(caseUrn);

        assertEquals(caseUrn, caseDetails.caseUrn());
        assertEquals(1, caseDetails.courtSchedule().size());
        List<CaseDetailsHearingDto> hearings = caseDetails.courtSchedule().getFirst().hearings();
        assertEquals(4, hearings.size());

        assertEquals(HearingType.SENTENCE.getValue(), hearings.get(0).hearingType());
        assertEquals("1", hearings.get(0).hearingId());
        assertEquals(datePlus7, hearings.get(0).weekCommencingStartDate());
        assertEquals(datePlus14, hearings.get(0).weekCommencingEndDate());
        assertEquals("1", hearings.get(0).weekCommencingDurationInWeeks());
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
            null,
            null ,
            null,
            courtSittings
        );
    }

    private HearingDto createHearing(String hearingId, String hearingType, List<CourtSittingDto> courtSittings){
        return new HearingDto(
            hearingId,
            hearingType,
            hearingType,
            "Note1",
            null,
            null ,
            null,
            courtSittings
        );
    }

    private HearingDto createHearingWithWeeks(String hearingId, String hearingType,
                                              String weekCommencingStartDate, String weekCommencingEndDate,
                                              String weekCommencingDurationInWeeks,
                                              List<CourtSittingDto> courtSittings){
        return new HearingDto(
            hearingId,
            hearingType,
            hearingType,
            "Note1",
            weekCommencingStartDate,
            weekCommencingEndDate,
            weekCommencingDurationInWeeks,
            courtSittings
        );
    }
}
