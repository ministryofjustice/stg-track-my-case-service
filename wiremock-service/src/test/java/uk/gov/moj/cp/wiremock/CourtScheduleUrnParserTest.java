package uk.gov.moj.cp.wiremock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

class CourtScheduleUrnParserTest {

    private static Stream<Arguments> parseCaseUrnParams() {
        return Stream.of(
            // TMCTR* - Trial
            of("TMCTR99M99D", "Trial", 99, 99, 1),
            of("TMCTR99D", "Trial", 0, 99, 1),
            of("TMCTR99M", "Trial", 99, 0, 1),
            of("TMCTRN1D", "Trial", 0, -1, 1),
            of("TMCTRN9D", "Trial", 0, -9, 1),
            of("TMCTRN1D5", "Trial", 0, -1, 5),
            of("TMCTR1MN1D2", "Trial", 1, -1, 2),
            of("TMCTR0D", "Trial", 0, 0, 1),
            of("TMCTRN1D2", "Trial", 0, -1, 2),
            of("TMCTR11M99D", "Trial", 11, 99, 1),
            of("TMCTR0D1", "Trial", 0, 0, 1),
            of("TMCTR0D12", "Trial", 0, 0, 12),
            of("TMCTR0D123", "Trial", 0, 0, 123),
            // TMCSE* - Sentence
            of("TMCSE99M99D", "Sentence", 99, 99, 1),
            of("TMCSE99D", "Sentence", 0, 99, 1),
            of("TMCSE99M", "Sentence", 99, 0, 1),
            of("TMCSEN9M", "Sentence", -9, 0, 1),
            of("TMCSEN1D3", "Sentence", 0, -1, 3),
            of("TMCSEN2D5", "Sentence", 0, -2, 5),
            // Edge cases
            of("X", "Unknown", 0, 0, 1),
            of("TMCXX", "Unknown", 0, 0, 1),
            of("XXXTR", "Unknown", 0, 0, 1),
            of("XXXSE", "Unknown", 0, 0, 1),
            of("TMCTR", "Trial", 0, 0, 1),
            of("TMCSE", "Sentence", 0, 0, 1),
            of("TMCTRD", "Trial", 0, 0, 1),
            of("TMCTRM", "Trial", 0, 0, 1),
            of("TMCTRMD", "Trial", 0, 0, 1),
            of("TMCTRDM", "Trial", 0, 0, 1),
            of("TMCTRN", "Trial", 0, 0, 1),
            of("TMCTRND", "Trial", 0, 0, 1),
            of("TMCTRMN", "Trial", 0, 0, 1),
            of("TMCTRMN5", "Trial", 0, 0, 5),
            of("TMCTRMN5X", "Trial", 0, 0, 1),
            of("TMCTRN5DX", "Trial", 0, 0, 1),
            of("TMCTR5", "Trial", 0, 0, 5)
        );
    }

    @ParameterizedTest(name = "parse({0}) -> {1}, M={2}, D={3}, hearings={4}")
    @MethodSource("parseCaseUrnParams")
    void parseCaseUrnTest(String caseUrn, String expectedType, int expectedMonths, int expectedDays, int expectedTotalHearings) {
        CourtScheduleResponseTransformer.MockDataSummaryForTest summary =
            CourtScheduleResponseTransformer.parseCaseUrnForTest(caseUrn);
        assertEquals(expectedType, summary.hearingType().value, "hearingType");
        assertEquals(expectedMonths, summary.months(), "months");
        assertEquals(expectedDays, summary.days(), "days");
        assertEquals(expectedTotalHearings, summary.totalHearings(), "totalHearings");
    }
}

