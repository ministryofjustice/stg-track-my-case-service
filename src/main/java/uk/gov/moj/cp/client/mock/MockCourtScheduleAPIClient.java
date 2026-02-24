package uk.gov.moj.cp.client.mock;

import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtScheduleSchema;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import uk.gov.moj.cp.client.api.CourtScheduleClient;
import uk.gov.moj.cp.model.HearingType;
import uk.gov.moj.cp.model.mock.MockDataSummary;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ConditionalOnProperty(
    name = "services.use-mock-data",
    havingValue = "true"
)
public class MockCourtScheduleAPIClient implements CourtScheduleClient {

    public static final String MOCK_DATA_URN_PREFIX_TMC = "TMC";

    @Override
    public ResponseEntity<CourtScheduleSchema> getCourtScheduleByCaseUrn(String accessToken, String caseUrn) {

        return ResponseEntity.ok(new CourtScheduleSchema(customData(caseUrn)));
    }

    public List<CourtSchedule> customData(final String caseUrn) {
        MockDataSummary mockDataSummary = parseCaseUrn(caseUrn);
        return generateData(caseUrn, mockDataSummary);
    }

    private List<CourtSchedule> generateData(final String caseUrn, MockDataSummary mockDataSummary) {
        List<Hearing> hearings = new ArrayList<>();

        // Create test data

        ZonedDateTime futureDate = ZonedDateTime.now()
            .plusMonths(mockDataSummary.getMonths())
            .plusDays(mockDataSummary.getDays())
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);


        List<CourtSitting> courtSittings = new ArrayList<>();

        int days = mockDataSummary.getTotalHearings();
        for (int i = 0; i < days; i++) {
            ZonedDateTime sittingStart = futureDate;
            ZonedDateTime sittingEnd = futureDate.plusHours(1);

            final String judiciaryId = caseUrn + "-judiciary-id-" + (i + 1);
            final String courtHouseId = caseUrn + "-court-house-id-" + (i + 1);
            final String courtRoomId = caseUrn + "-court-room-id-" + (i + 1);

            final CourtSitting courtSitting = new CourtSitting(
                sittingStart,
                sittingEnd,
                judiciaryId,
                courtHouseId,
                courtRoomId
            );
            courtSittings.add(courtSitting);

            futureDate = futureDate.plusDays(1);
        }

        final String hearingType = mockDataSummary.getHearingType().getValue();

        final String hearingId = caseUrn + "-hearing-id";

        final Hearing hearing = new Hearing(
            hearingId,
            hearingType,
            "Follow-up " + hearingType.toLowerCase() + " hearing description for case " + caseUrn,
            "Note for first hearing",
            null,
            courtSittings
        );

        hearings.add(hearing);
        return List.of(new CourtSchedule(hearings));
    }

    public static MockDataSummary parseCaseUrn(final String caseUrn) {
        //    TMCTR99M99D / TMCTR99D / TMCTR99M / TMCTRN9D / TMCTRN1D5
        //    TMCSE99M99D / TMCSE99D / TMCSE99M / TMCSEN9M / TMCSEN1D3
        //    TMC - is custom test prefix
        //    TR or SE - Trial or Sentence type of hearing
        //    99M - maximum 2 digits number of months (optional)
        //    99D - maximum 2 digits number of days (optional)
        //    N9D - negative 9 days, N9M - negative 9 months
        //    2-999 number at the end - multi day hearing (totalHearings, 1–3 digits)
        //
        //    TMCTR0D - today single trial hearing
        //    TMCTRN1D2 - trial hearing started 1 day before and has multi day hearing (for 2 days)
        //    TMCSEN2D5 - sentencing hearing started 2 days before and has multi day hearing (for 5 days)
        //
        //    TMCTRV1 /  TMCTRV21 - trial hearing with custom data, id=1 / id=21 (future feature, like multi hearing)

        if (caseUrn == null || caseUrn.length() < 5 || !caseUrn.toUpperCase().startsWith(MOCK_DATA_URN_PREFIX_TMC)) {
            return defaultSummary();
        }
        String rest = caseUrn.substring(MOCK_DATA_URN_PREFIX_TMC.length());
        HearingType hearingType = rest.startsWith(HearingType.TRIAL.getValue().substring(
            0,
            2
        ).toUpperCase()) ? HearingType.TRIAL
            : rest.startsWith(HearingType.SENTENCE.getValue().substring(0, 2).toUpperCase()) ? HearingType.SENTENCE
            : null;
        if (hearingType == null) {
            return defaultSummary();
        }
        String body = rest.substring(2);
        int totalHearings = 1;
        if (!body.isEmpty()) {
            int i = body.length();
            while (i > 0 && Character.isDigit(body.charAt(i - 1))) {
                i--;
            }
            if (i < body.length()) {
                int suffix = Integer.parseInt(body.substring(i), 10);
                if (suffix >= 2) {
                    totalHearings = suffix;
                    body = body.substring(0, i);
                }
            }
        }
        int months = 0;
        int days = 0;
        Pattern bodyPattern = Pattern.compile("^(N?\\d{1,5}M)?(N?\\d{1,5}D)?$");
        Matcher matcher = bodyPattern.matcher(body);
        if (matcher.matches()) {
            months = parseSignedUnit(matcher.group(1));
            days = parseSignedUnit(matcher.group(2));
        }
        return MockDataSummary.builder()
            .hearingType(hearingType)
            .months(months)
            .days(days)
            .totalHearings(totalHearings)
            .build();
    }

    private static int parseSignedUnit(String group) {
        if (group == null || group.isEmpty()) {
            return 0;
        }
        boolean negative = group.startsWith("N");
        String digits = negative ? group.substring(1, group.length() - 1) : group.substring(0, group.length() - 1);
        int value = Integer.parseInt(digits, 10);
        return negative ? -value : value;
    }

    private static MockDataSummary defaultSummary() {
        return MockDataSummary.builder()
            .hearingType(null)
            .months(0)
            .days(0)
            .totalHearings(1)
            .build();
    }

}
