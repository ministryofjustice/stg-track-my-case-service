package uk.gov.moj.cp.wiremock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Response;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CourtScheduleResponseTransformer implements ResponseTransformerV2 {

    public static final String NAME = "court-schedule-transformer";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final String MOCK_DATA_URN_PREFIX_TMC = "TMC";

    static MockDataSummaryForTest parseCaseUrnForTest(final String caseUrn) {
        return parseCaseUrn(caseUrn);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        String url = serveEvent.getRequest().getUrl(); // includes query string if any

        String caseUrn = extractCaseUrn(url);
        MockDataSummaryForTest summary = parseCaseUrn(caseUrn);

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode courtSchedule = root.putArray("courtSchedule");

        ObjectNode schedule = MAPPER.createObjectNode();
        ArrayNode hearings = schedule.putArray("hearings");

        ObjectNode hearing = MAPPER.createObjectNode();
        String hearingType = summary.hearingType.value;
        hearing.put("hearingId", caseUrn + "-hearing-id");
        hearing.put("hearingType", hearingType);
        hearing.put("hearingDescription", "Follow-up " + hearingType.toLowerCase() + " hearing description for case " + caseUrn);
        hearing.put("listNote", "Note for first hearing");
        hearing.putNull("weekCommencing");

        ArrayNode courtSittings = hearing.putArray("courtSittings");

        ZonedDateTime futureDate = ZonedDateTime.now()
            .plusMonths(summary.months)
            .plusDays(summary.days)
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

        for (int i = 0; i < summary.totalHearings; i++) {
            ZonedDateTime sittingStart = futureDate;
            ZonedDateTime sittingEnd = futureDate.plusHours(1);

            ObjectNode sitting = MAPPER.createObjectNode();
            sitting.put("sittingStart", ISO_OFFSET.format(sittingStart));
            sitting.put("sittingEnd", ISO_OFFSET.format(sittingEnd));
            sitting.put("judiciaryId", caseUrn + "-judiciary-id-" + (i + 1));
            sitting.put("courtHouse", caseUrn + "-court-house-id-" + (i + 1));
            sitting.put("courtRoom", caseUrn + "-court-room-id-" + (i + 1));
            courtSittings.add(sitting);

            futureDate = futureDate.plusDays(1);
        }

        hearings.add(hearing);
        courtSchedule.add(schedule);

        return Response.Builder.like(response)
            .but()
            .status(200)
            .headers(new HttpHeaders(new HttpHeader("Content-Type", "application/json")))
            .body(root.toString())
            .build();
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private static String extractCaseUrn(String url) {
        // expected: /case/{case_urn}/courtschedule
        Pattern p = Pattern.compile("^/case/([^/?]+)/courtschedule(?:\\?.*)?$");
        Matcher m = p.matcher(url);
        return m.matches() ? m.group(1) : "UNKNOWN";
    }

    private static MockDataSummaryForTest parseCaseUrn(final String caseUrn) {
        if (caseUrn == null || caseUrn.length() < 5 || !caseUrn.toUpperCase().startsWith(MOCK_DATA_URN_PREFIX_TMC)) {
            return defaultSummary();
        }
        String rest = caseUrn.substring(MOCK_DATA_URN_PREFIX_TMC.length());
        MockHearingType hearingType = rest.startsWith("TR") ? MockHearingType.TRIAL
            : rest.startsWith("SE") ? MockHearingType.SENTENCE
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

        return new MockDataSummaryForTest(hearingType, months, days, totalHearings);
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

    private static MockDataSummaryForTest defaultSummary() {
        return new MockDataSummaryForTest(MockHearingType.UNKNOWN, 0, 0, 1);
    }

    enum MockHearingType {
        TRIAL("Trial"),
        SENTENCE("Sentence"),
        UNKNOWN("Unknown");

        final String value;

        MockHearingType(String value) {
            this.value = value;
        }
    }

    record MockDataSummaryForTest(MockHearingType hearingType, int months, int days, int totalHearings) {}
}

