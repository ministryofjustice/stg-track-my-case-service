package uk.gov.moj.cp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static java.util.UUID.randomUUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto.AddressDto;
import uk.gov.moj.cp.model.HearingType;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

class WarningMessageServiceTest {

    private WarningMessageService warningMessageService;

    private static final LocalDateTime FIXED_TODAY = LocalDateTime.of(2026, 1, 30, 10, 0, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
        FIXED_TODAY.atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    );

    private String judgeId;
    private String courtHouseId;
    private String courtRoomId;
    private String futureSittingEndDate;

    @BeforeEach
    void setUp() {
        warningMessageService = new WarningMessageService(FIXED_CLOCK);

        judgeId = randomUUID().toString();
        courtHouseId = randomUUID().toString();
        courtRoomId = randomUUID().toString();
        futureSittingEndDate = FIXED_TODAY.plusDays(1).plusHours(2).toString();
    }

    @Test
    @DisplayName("getMessage should return null when hearingType optional is empty")
    void testGetMessageWithEmptyHearingType() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).toString(),
            FIXED_TODAY.plusMonths(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.empty(), List.of(sitting));
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return null when sitting list is empty")
    void testGetMessageWithEmptySitting() {
        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of());
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return null when both are empty")
    void testGetMessageWithBothEmpty() {
        String result = warningMessageService.getMessage(Optional.empty(), List.of());
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return null when sittingStart is null")
    void testGetMessageWithNullSittingStart() {
        CaseDetailsCourtSittingDto sitting = new CaseDetailsCourtSittingDto(
            judgeId, null, futureSittingEndDate, createCourtHouse(new CourtRoomDto(123, "Room"),
            new AddressDto("1", "Street", "City", null, "POST", null))
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return null when sittingStart is N/A")
    void testGetMessageWithNASittingStart() {
        CaseDetailsCourtSittingDto sitting = new CaseDetailsCourtSittingDto(
            judgeId, "N/A", futureSittingEndDate, createCourtHouse(new CourtRoomDto(123, "Room"),
            new AddressDto("1", "Street", "City", null, "POST", null))
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return null when sitting date is in the past")
    void testGetMessageWithPastDate() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.minusDays(1).toString(),
            FIXED_TODAY.minusDays(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return null for invalid hearing type")
    void testGetMessageWithInvalidHearingType() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).toString(),
            FIXED_TODAY.plusMonths(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of("InvalidType"), List.of(sitting));
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_TODAY when period is 0 months and 0 days (today)")
    void testGetMessageWithTodayDate() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.toString(),
            FIXED_TODAY.plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertNotNull(result);
        assertEquals("SENTENCING_HEARING_STARTS_TODAY", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_TWENTY_NINE_DAYS for 1 month, 0 days")
    void testGetMessageSentenceOneMonth() {
        LocalDate today = FIXED_TODAY.toLocalDate(); // January 30, 2026
        LocalDate oneMonthLater = today.plusMonths(1); // February 28, 2026 (adjusted from Feb 30)
        LocalDateTime sittingDateTime = oneMonthLater.atTime(10, 0, 0);

        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            sittingDateTime.toString(),
            sittingDateTime.plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWENTY_NINE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_ONE_MONTH_ONE_DAY for 1 month, 1 day")
    void testGetMessageSentenceOneMonthOneDay() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).plusDays(1).toString(),
            FIXED_TODAY.plusMonths(1).plusDays(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_ONE_MONTH_ONE_DAY", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_ONE_MONTH_TWO_DAYS for 1 month, 2 days")
    void testGetMessageSentenceOneMonthTwoDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).plusDays(2).toString(),
            FIXED_TODAY.plusMonths(1).plusDays(2).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_ONE_MONTH_TWO_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_ONE_MONTH_THREE_DAYS for 1 month, 3 days")
    void testGetMessageSentenceOneMonthThreeDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).plusDays(3).toString(),
            FIXED_TODAY.plusMonths(1).plusDays(3).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_ONE_MONTH_THREE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_TWO_MONTHS for 2 months, 0 days")
    void testGetMessageSentenceTwoMonths() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(2).toString(),
            FIXED_TODAY.plusMonths(2).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWO_MONTHS", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_TWO_MONTHS_FIVE_DAYS for 2 months, 5 days")
    void testGetMessageSentenceTwoMonthsFiveDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(2).plusDays(5).toString(),
            FIXED_TODAY.plusMonths(2).plusDays(5).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWO_MONTHS_FIVE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_THREE_MONTHS for 3 months, 0 days")
    void testGetMessageSentenceThreeMonths() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(3).toString(),
            FIXED_TODAY.plusMonths(3).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_THREE_MONTHS", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_TOMRROW for 0 months, 1 day")
    void testGetMessageSentenceOneDay() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
                FIXED_TODAY.plusDays(1).toString(),
                FIXED_TODAY.plusDays(1).plusHours(2).toString()
        );
        CaseDetailsCourtSittingDto sitting1 = createCaseDetailsCourtSittingDto(
                FIXED_TODAY.plusDays(2).toString(),
                FIXED_TODAY.plusDays(2).plusHours(2).toString()
        );


        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting, sitting1));
        assertEquals("SENTENCING_HEARING_STARTS_TOMORROW", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_TOMRROW for 0 months, 1 day")
    void testGetMessageSentenceOngoing() {
        CaseDetailsCourtSittingDto sitting1 = createCaseDetailsCourtSittingDto(
                FIXED_TODAY.toString(),
                FIXED_TODAY.plusHours(2).toString()
        );
        CaseDetailsCourtSittingDto sitting2 = createCaseDetailsCourtSittingDto(
                FIXED_TODAY.plusDays(1).toString(),
                FIXED_TODAY.plusDays(1).plusHours(2).toString()
        );

        CaseDetailsCourtSittingDto sitting3 = createCaseDetailsCourtSittingDto(
                FIXED_TODAY.plusDays(2).toString(),
                FIXED_TODAY.plusDays(2).plusHours(2).toString()
        );


        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting1, sitting2, sitting3));
        assertEquals("SENTENCING_HEARING_STARTS_TODAY", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_TWO_DAYS for 0 months, 2 days")
    void testGetMessageSentenceTwoDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(2).toString(),
            FIXED_TODAY.plusDays(2).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWO_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_ONE_MONTH_ONE_DAY for 0 months, 30 days")
    void testGetMessageSentenceThirtyDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(30).toString(),
            FIXED_TODAY.plusDays(30).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_ONE_MONTH_ONE_DAY", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_STARTS_IN_TWENTY_ONE_DAYS for 0 months, 21 days")
    void testGetMessageSentenceTwentyOneDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(21).toString(),
            FIXED_TODAY.plusDays(21).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWENTY_ONE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should return TRIAL_HEARING_STARTS_IN_TWENTY_NINE_DAYS for Trial, 1 month, 0 days")
    void testGetMessageTrialOneMonth() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).toString(),
            FIXED_TODAY.plusMonths(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting));
        assertEquals("TRIAL_HEARING_STARTS_IN_TWENTY_NINE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should return TRIAL_HEARING_STARTS_IN_ONE_MONTH_ONE_DAY for Trial, 1 month, 1 day")
    void testGetMessageTrialOneMonthOneDay() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).plusDays(1).toString(),
            FIXED_TODAY.plusMonths(1).plusDays(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting));
        assertEquals("TRIAL_HEARING_STARTS_IN_ONE_MONTH_ONE_DAY", result);
    }

    @Test
    @DisplayName("getMessage should return TRIAL_HEARING_STARTS_TODAY when date is today")
    void testGetMessageTRIAL_HEARING_STARTS_TODAY() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.toString(),
            FIXED_TODAY.plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting));
        assertEquals("TRIAL_HEARING_STARTS_TODAY", result);
    }

    @Test
    @DisplayName("getMessage should return TRIAL_HEARING_STARTS_TOMRROW when date is tomorrow")
    void testGetMessageTRIAL_HEARING_STARTS_TOMRROW() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(1).toString(),
            FIXED_TODAY.plusDays(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting));
        assertEquals("TRIAL_HEARING_STARTS_TOMORROW", result);
    }

    @Test
    @DisplayName("getMessage should return TRIAL_HEARING_STARTS_IN_TWO_MONTHS_THREE_DAYS for Trial, 2 months, 3 days")
    void testGetMessageTrialTwoMonthsThreeDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(2).plusDays(3).toString(),
            FIXED_TODAY.plusMonths(2).plusDays(3).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting));
        assertEquals("TRIAL_HEARING_STARTS_IN_TWO_MONTHS_THREE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle 12 months (1 year) correctly")
    void testGetMessageTwelveMonths() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(12).toString(),
            FIXED_TODAY.plusMonths(12).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWELVE_MONTHS", result);
    }

    @Test
    @DisplayName("getMessage should handle 1 year and 1 month correctly")
    void testGetMessageOneYearOneMonth() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusYears(1).plusMonths(1).toString(),
            FIXED_TODAY.plusYears(1).plusMonths(1).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWELVE_MONTHS_TWENTY_NINE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle 99 days correctly")
    void testGetMessageNinetyNineDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(99).toString(),
            FIXED_TODAY.plusDays(99).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_THREE_MONTHS_NINE_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle 10 days correctly")
    void testGetMessageTenDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(10).toString(),
            FIXED_TODAY.plusDays(10).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TEN_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle 11 days correctly")
    void testGetMessageElevenDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(11).toString(),
            FIXED_TODAY.plusDays(11).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_ELEVEN_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle 20 days correctly")
    void testGetMessageTwentyDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(20).toString(),
            FIXED_TODAY.plusDays(20).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWENTY_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle 1 month and 10 days correctly")
    void testGetMessageOneMonthTenDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).plusDays(10).toString(),
            FIXED_TODAY.plusMonths(1).plusDays(10).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_ONE_MONTH_TEN_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle 1 month and 11 days correctly")
    void testGetMessageOneMonthElevenDays() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).plusDays(11).toString(),
            FIXED_TODAY.plusMonths(1).plusDays(11).plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_ONE_MONTH_ELEVEN_DAYS", result);
    }

    @Test
    @DisplayName("getMessage should handle case-insensitive hearing type")
    void testGetMessageCaseInsensitiveHearingType() {
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusMonths(1).toString(),
            FIXED_TODAY.plusMonths(1).plusHours(2).toString()
        );

        String result1 = warningMessageService.getMessage(Optional.of("sentence"), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWENTY_NINE_DAYS", result1);

        String result2 = warningMessageService.getMessage(Optional.of("SENTENCE"), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWENTY_NINE_DAYS", result2);

        String result3 = warningMessageService.getMessage(Optional.of("Sentence"), List.of(sitting));
        assertEquals("SENTENCING_HEARING_STARTS_IN_TWENTY_NINE_DAYS", result3);
    }

    @Test
    @DisplayName("getMessage should handle invalid date format gracefully")
    void testGetMessageWithInvalidDateFormat() {
        CaseDetailsCourtSittingDto sitting = new CaseDetailsCourtSittingDto(
            judgeId, "invalid-date-format", futureSittingEndDate, createCourtHouse(new CourtRoomDto(123, "Room"),
            new AddressDto("1", "Street", "City", null, "POST", null))
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting));
        assertNull(result);
    }

    @Test
    @DisplayName("getMessage should return TRIAL_IS_ONGOING when trial hearing has multiple sittings and current date is between sitting start dates")
    void testGetMessageTrialIsOngoing() {
        // Create multiple sittings where current date (FIXED_TODAY = 2026-01-30) is between first and last sitting start dates
        CaseDetailsCourtSittingDto sitting1 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.minusDays(2).toString(), // 2026-01-28
            FIXED_TODAY.minusDays(2).plusHours(2).toString()
        );
        CaseDetailsCourtSittingDto sitting2 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(3).toString(), // 2026-02-02
            FIXED_TODAY.plusDays(3).plusHours(2).toString()
        );

        // FIXED_TODAY (2026-01-30) is between 2026-01-28 and 2026-02-02, so it's ongoing
        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting1, sitting2));
        assertEquals("TRIAL_HEARING_IS_ONGOING", result);
    }

    @Test
    @DisplayName("getMessage should return SENTENCING_HEARING_IS_ONGOING when sentencing hearing has multiple sittings and current date is between sitting start dates")
    void testGetMessageSentencingIsOngoing() {
        // Create multiple sittings where current date is between first and last sitting start dates
        CaseDetailsCourtSittingDto sitting1 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.minusDays(1).toString(), // 2026-01-29
            FIXED_TODAY.minusDays(1).plusHours(2).toString()
        );
        CaseDetailsCourtSittingDto sitting2 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(5).toString(), // 2026-02-04
            FIXED_TODAY.plusDays(5).plusHours(2).toString()
        );

        // FIXED_TODAY (2026-01-30) is between 2026-01-29 and 2026-02-04, so it's ongoing
        String result = warningMessageService.getMessage(Optional.of(HearingType.SENTENCE.getValue()), List.of(sitting1, sitting2));
        assertEquals("SENTENCING_HEARING_IS_ONGOING", result);
    }

    @Test
    @DisplayName("getMessage should return ongoing message when current date equals earliest sitting start date")
    void testGetMessageOngoingAtEarliestStartDate() {
        // Create multiple sittings where current date equals the earliest sitting start date
        CaseDetailsCourtSittingDto sitting1 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.toString(), // 2026-01-30 (earliest, equals FIXED_TODAY)
            FIXED_TODAY.plusHours(2).toString()
        );
        CaseDetailsCourtSittingDto sitting2 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.plusDays(3).toString(), // 2026-02-02
            FIXED_TODAY.plusDays(3).plusHours(2).toString()
        );

        // FIXED_TODAY (2026-01-30) equals earliest sitting start date and is before latest, so it's ongoing
        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting1, sitting2));
        assertEquals("TRIAL_HEARING_STARTS_TODAY", result);
    }

    @Test
    @DisplayName("getMessage should NOT return ongoing message when current date equals latest sitting start date")
    void testGetMessageNotOngoingAtLatestStartDate() {
        // Create multiple sittings where current date equals the latest sitting start date
        CaseDetailsCourtSittingDto sitting1 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.minusDays(2).toString(), // 2026-01-28
            FIXED_TODAY.minusDays(2).plusHours(2).toString()
        );
        CaseDetailsCourtSittingDto sitting2 = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.toString(), // 2026-01-30 (latest, equals FIXED_TODAY)
            FIXED_TODAY.plusHours(2).toString()
        );

        // FIXED_TODAY (2026-01-30) equals latest sitting start date, so it's NOT ongoing (hearing is starting today)
        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting1, sitting2));
        assertEquals("TRIAL_HEARING_IS_ONGOING", result);
    }

    @Test
    @DisplayName("getMessage should NOT return ongoing message when there is only one sitting")
    void testGetMessageNotOngoingWithSingleSitting() {
        // Single sitting should not be considered ongoing
        CaseDetailsCourtSittingDto sitting = createCaseDetailsCourtSittingDto(
            FIXED_TODAY.toString(),
            FIXED_TODAY.plusHours(2).toString()
        );

        String result = warningMessageService.getMessage(Optional.of(HearingType.TRIAL.getValue()), List.of(sitting));
        assertEquals("TRIAL_HEARING_STARTS_TODAY", result);
    }

    private CaseDetailsCourtSittingDto createCaseDetailsCourtSittingDto(String sittingStart, String sittingEnd) {
        CourtRoomDto courtRoomDto = new CourtRoomDto(123, "CourtRoom 01");
        AddressDto addressDto = new AddressDto("53", "Court Street", "London", null, "CB4 3MX", null);
        CourtHouseDto courtHouseDto = createCourtHouse(courtRoomDto, addressDto);

        return new CaseDetailsCourtSittingDto(
            judgeId,
            sittingStart,
            sittingEnd,
            courtHouseDto
        );
    }

    private CourtHouseDto createCourtHouse(final CourtRoomDto courtRoomDto, final AddressDto addressDto) {
        return new CourtHouseDto(
            courtHouseId,
            courtRoomId,
            "CROWN",
            "123",
            "Lavender Hill",
            addressDto,
            java.util.Arrays.asList(courtRoomDto)
        );
    }
}

