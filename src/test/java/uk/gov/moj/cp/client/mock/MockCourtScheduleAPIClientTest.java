package uk.gov.moj.cp.client.mock;

import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cp.model.HearingType;
import uk.gov.moj.cp.model.mock.MockDataSummary;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static uk.gov.moj.cp.model.HearingType.SENTENCE;
import static uk.gov.moj.cp.model.HearingType.TRIAL;
import static uk.gov.moj.cp.model.HearingType.UNKNOWN;
import static uk.gov.moj.cp.client.mock.MockCourtScheduleAPIClient.parseCaseUrn;

class MockCourtScheduleAPIClientTest {

    private final MockCourtScheduleAPIClient mockCourtScheduleClient = new MockCourtScheduleAPIClient();

    //    TMC - custom test prefix
    //    TR or SE - Trial or Sentence
    //    99M - max 2 digits months (optional)
    //    99D - max 2 digits days (optional)
    //    N9D - negative 9 days, N9M - negative 9 months
    //    1-999 at end (or 1+ after M/D) - multi-day hearing; N1, N1D, N1D1 → −1d offset, 1 sitting
    private static Stream<Arguments> parseCaseUrnParams() {
        return Stream.of(
            // TMCTR* - Trial
            // negative start date
            of("TMCTRN", TRIAL, 0, -1, 1),
            of("TMCTRN1", TRIAL, 0, -1, 1),
            of("TMCTRN1D", TRIAL, 0, -1, 1),
            of("TMCTRN1D1", TRIAL, 0, -1, 1),
            of("TMCTRN1D0", TRIAL, 0, -1, 0), // N1D + 0 sittings; offset still −1d
            of("TMCTRN1D2", TRIAL, 0, -1, 2), // trial 1 day before, multi-day 2 days

            of("TMCTRN1M", TRIAL, -1, 0, 1),
            of("TMCTRN1M2", TRIAL, -1, 0, 2),
            of("TMCTRN1M1D", TRIAL, -1, -1, 1),  // compact N1M1D: -1M and -1D
            of("TMCTRN1M1D2", TRIAL, -1, -1, 2), // +2 sittings

            of("TMCTR99M99D", TRIAL, 99, 99, 1),
            of("TMCTR99D", TRIAL, 0, 99, 1),
            of("TMCTR99M", TRIAL, 99, 0, 1),
            of("TMCTRN1D5", TRIAL, 0, -1, 5),
            of("TMCTRN9D", TRIAL, 0, -9, 1),
            of("TMCTR1MN1D2", TRIAL, 1, -1, 2),
            of("TMCTR0D", TRIAL, 0, 0, 1),   // today single trial
            of("TMCTR11M99D", TRIAL, 11, 99, 1),
            of("TMCTR0D1", TRIAL, 0, 0, 1),
            of("TMCTR0D12", TRIAL, 0, 0, 12),
            of("TMCTR0D123", TRIAL, 0, 0, 123),
            // TMCSE* - Sentence
            of("TMCSEN", SENTENCE, 0, -1, 1), // same as TMCSEN1 when N has no digit
            of("TMCSE99M99D", SENTENCE, 99, 99, 1),
            of("TMCSEMD", SENTENCE, 1, 1, 1),
            of("TMCSE99D", SENTENCE, 0, 99, 1),
            of("TMCSE99M", SENTENCE, 99, 0, 1),
            of("TMCSEN9M", SENTENCE, -9, 0, 1),
            of("TMCSEN1D3", SENTENCE, 0, -1, 3),
            of("TMCSEN2D5", SENTENCE, 0, -2, 5),  // sentence 2 days before, multi-day 5 days
            // Edge cases
            of("X", UNKNOWN, 0, 0, 1),
            of("TMCXX", UNKNOWN, 0, 0, 1),
            of("XXXTR", UNKNOWN, 0, 0, 1),
            of("XXXSE", UNKNOWN, 0, 0, 1),
            of("TMCTR", TRIAL, 0, 0, 1),
            of("TMCSE", SENTENCE, 0, 0, 1),
            of("TMCTRD", TRIAL, 0, 0, 1),
            of("TMCTRMD", TRIAL, 1, 1, 1), // MD with no digits → 1M, 1D, 1 hearing
            of("TMCTRM", TRIAL, 0, 0, 1),
            of("TMCTRDM", TRIAL, 0, 0, 1),
            of("TMCTRDX", TRIAL, 0, 0, 1),
            of("TMCTRND", TRIAL, 0, 0, 1),
            of("TMCTRMN", TRIAL, 0, 0, 1),
            of("TMCTRMN5", TRIAL, 0, 0, 5),
            of("TMCTRMN5X", TRIAL, 0, 0, 1),
            of("TMCTRN5DX", TRIAL, 0, 0, 1),
            of("TMCTR5", TRIAL, 0, 0, 5),
            of("TMCTRXXX", TRIAL, 0, 0, 1),
            of("TMCSEXXX", SENTENCE, 0, 0, 1)
            );
    }

    @ParameterizedTest(name = "parseCaseUrn({0}) -> type={1}, M={2}, D={3}, hearings={4}")
    @MethodSource("parseCaseUrnParams")
    void parseCaseUrnTest(String caseUrn, HearingType expectedType, int expectedMonths, int expectedDays, int expectedTotalHearings) {
        MockDataSummary summary = parseCaseUrn(caseUrn);
        assertEquals(expectedType, summary.getHearingType(),
                     format("caseUrn: %s, hearingType => was: %s, got: %s", caseUrn, summary.getHearingType(), expectedType));
        assertEquals(expectedMonths, summary.getMonths(),
                     format("caseUrn: %s, months => was: %s, got: %s", caseUrn, summary.getMonths(), expectedMonths));
        assertEquals(expectedDays, summary.getDays(),
                     format("caseUrn: %s, days => was: %s, got: %s", caseUrn, summary.getDays(), expectedDays));
        assertEquals(expectedTotalHearings, summary.getTotalHearings(),
                     format("caseUrn: %s, totalHearings => was: %s, got: %s", caseUrn, summary.getTotalHearings(), expectedTotalHearings));
    }

    @Test
    void testCustomData_tmcse1m2d12() {
        List<CourtSchedule> courtSchedules = mockCourtScheduleClient.customData("TMCSE1M2D12");
        assertEquals(1, courtSchedules.size());
        List<Hearing> hearings = courtSchedules.getFirst().getHearings();
        assertEquals(1, hearings.size());
        Hearing hearing = hearings.getFirst();
        assertEquals(12, hearing.getCourtSittings().size());
    }

    @Test
    void customData_N1variants_firstSittingIsInThePast() {
        ZonedDateTime now = ZonedDateTime.now();
        for (String urn : List.of("TMCTRN", "TMCTRN1", "TMCTRN1D", "TMCTRN1D1")) {
            List<CourtSchedule> courtSchedules = mockCourtScheduleClient.customData(urn);
            CourtSitting firstSitting = courtSchedules.getFirst().getHearings().getFirst()
                .getCourtSittings().getFirst();
            assertTrue(
                firstSitting.getSittingStart().isBefore(now),
                "expected past sitting for " + urn
            );
        }
    }
}
