package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.Hearing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cp.model.HearingType;
import uk.gov.moj.cp.model.mock.MockDataSummary;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;
import static uk.gov.moj.cp.model.HearingType.SENTENCE;
import static uk.gov.moj.cp.model.HearingType.TRIAL;
//import static uk.gov.moj.cp.service.MockCourtScheduleClient.parseCaseUrn;

class MockCourtScheduleAPIClientTest {

    //private final MockCourtScheduleClient mockCourtScheduleClient = new MockCourtScheduleClient();

    //    TMC - custom test prefix
    //    TR or SE - Trial or Sentence
    //    99M - max 2 digits months (optional)
    //    99D - max 2 digits days (optional)
    //    N9D - negative 9 days, N9M - negative 9 months
    //    1-999 at end - multi-day hearing (totalHearings, 1–3 digits)
    private static Stream<Arguments> parseCaseUrnParams() {
        return Stream.of(
            // TMCTR* - Trial
            of("TMCTR99M99D", TRIAL, 99, 99, 1),
            of("TMCTR99D", TRIAL, 0, 99, 1),
            of("TMCTR99M", TRIAL, 99, 0, 1),
            of("TMCTRN1D", TRIAL, 0, -1, 1),
            of("TMCTRN9D", TRIAL, 0, -9, 1),
            of("TMCTRN1D5", TRIAL, 0, -1, 5),
            of("TMCTR1MN1D2", TRIAL, 1, -1, 2),
            of("TMCTR0D", TRIAL, 0, 0, 1),   // today single trial
            of("TMCTRN1D2", TRIAL, 0, -1, 2), // trial 1 day before, multi-day 2 days
            of("TMCTR11M99D", TRIAL, 11, 99, 1),
            of("TMCTR0D1", TRIAL, 0, 0, 1),
            of("TMCTR0D12", TRIAL, 0, 0, 12),
            of("TMCTR0D123", TRIAL, 0, 0, 123),
            // TMCSE* - Sentence
            of("TMCSE99M99D", SENTENCE, 99, 99, 1),
            of("TMCSE99D", SENTENCE, 0, 99, 1),
            of("TMCSE99M", SENTENCE, 99, 0, 1),
            of("TMCSEN9M", SENTENCE, -9, 0, 1),
            of("TMCSEN1D3", SENTENCE, 0, -1, 3),
            of("TMCSEN2D5", SENTENCE, 0, -2, 5),  // sentence 2 days before, multi-day 5 days
            // Edge cases
            of("X", null, 0, 0, 1),
            of("TMCXX", null, 0, 0, 1),
            of("XXXTR", null, 0, 0, 1),
            of("XXXSE", null, 0, 0, 1),
            of("TMCTR", TRIAL, 0, 0, 1),
            of("TMCSE", SENTENCE, 0, 0, 1),
            of("TMCTRD", TRIAL, 0, 0, 1),
            of("TMCTRM", TRIAL, 0, 0, 1),
            of("TMCTRMD", TRIAL, 0, 0, 1),
            of("TMCTRDM", TRIAL, 0, 0, 1),
            of("TMCTRN", TRIAL, 0, 0, 1),
            of("TMCTRND", TRIAL, 0, 0, 1),
            of("TMCTRMN", TRIAL, 0, 0, 1),
            of("TMCTRMN5", TRIAL, 0, 0, 5),
            of("TMCTRMN5X", TRIAL, 0, 0, 1),
            of("TMCTRN5DX", TRIAL, 0, 0, 1),
            of("TMCTR5", TRIAL, 0, 0, 5)
        );
    }

    /*@ParameterizedTest(name = "parseCaseUrn({0}) -> {1}, M={2}, D={3}, hearings={4}")
    @MethodSource("parseCaseUrnParams")
    void parseCaseUrnTest(String caseUrn, HearingType expectedType, int expectedMonths, int expectedDays, int expectedTotalHearings) {
        MockDataSummary summary = parseCaseUrn(caseUrn);
        assertEquals(expectedType, summary.getHearingType(), "hearingType");
        assertEquals(expectedMonths, summary.getMonths(), "months");
        assertEquals(expectedDays, summary.getDays(), "days");
        assertEquals(expectedTotalHearings, summary.getTotalHearings(), "totalHearings");
    }

    @Test
    void testCustomData_TMCSE1M2D12() {
        List<CourtSchedule> courtSchedules = mockCourtScheduleClient.customData("TMCSE1M2D12");
        assertEquals(1, courtSchedules.size());
        List<Hearing> hearings = courtSchedules.getFirst().getHearings();
        assertEquals(1, hearings.size());
        Hearing hearing = hearings.getFirst();
        assertEquals(12, hearing.getCourtSittings().size());
    }*/
}
