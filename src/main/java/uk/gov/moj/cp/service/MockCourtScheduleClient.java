package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.Address;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtHouse.CourtHouseType;
import com.moj.generated.hmcts.CourtRoom;
import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.moj.cp.model.HearingType;
import uk.gov.moj.cp.model.mock.MockDataSummary;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MockCourtScheduleClient {

    public static final String MOCK_DATA_URN_PREFIX_TMC = "TMC";

    @Getter
    @Value("${services.use-mock-data}")
    private Boolean useMockData;

    public boolean useMock(final String caseUrn) {
        return Boolean.TRUE.equals(getUseMockData()) && (caseUrn.toUpperCase().startsWith(MOCK_DATA_URN_PREFIX_TMC));
    }

    public List<CourtSchedule> customData(final String caseUrn) {
        MockDataSummary mockDataSummary = parseCaseUrn(caseUrn);
        return generateData(caseUrn, mockDataSummary);
    }

    private List<CourtSchedule> generateData(final String caseUrn, MockDataSummary mockDataSummary) {
        List<Hearing> hearings = new ArrayList<>();

        // Create test data

        LocalDateTime futureDate = LocalDateTime.now()
            .plusMonths(mockDataSummary.getMonths())
            .plusDays(mockDataSummary.getDays())
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);


        List<CourtSitting> courtSittings = new ArrayList<>();

        int days = mockDataSummary.getTotalHearings();
        for (int i = 0; i < days; i++) {
            Date sittingStart = Date.from(futureDate.atZone(getZoneId()).toInstant());
            Date sittingEnd = Date.from(futureDate.plusHours(1).atZone(getZoneId()).toInstant());

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

//[ {
//            "hearings" : [ {
//                "hearingId" : "810deb14-9a7d-4d3e-87f0-c37d4c832820",
//                    "hearingType" : "Trial",
//                    "hearingDescription" : "Trial",
//                    "listNote" : "",
//                    "weekCommencingDurationInWeeks" : 0,
//                    "courtSittings" : [ {
//                    "sittingStart" : 1773309600000,
//                        "sittingEnd" : 1773311400000,
//                        "judiciaryId" : "",
//                        "courtHouse" : "67aa82ba-67bb-4699-8176-5f572048352b",
//                        "courtRoom" : "c5f066ff-0aff-3dde-8bd9-40a587d3c58e"
//                } ]
//            }, {
//                "hearingId" : "1e04d449-04eb-4f72-8d8e-b658a278e4dc",
//                    "hearingType" : "First hearing",
//                    "hearingDescription" : "First hearing",
//                    "listNote" : "",
//                    "weekCommencingDurationInWeeks" : 0,
//                    "courtSittings" : [ {
//                    "sittingStart" : 1770718200000,
//                        "sittingEnd" : 1770719400000,
//                        "judiciaryId" : "",
//                        "courtHouse" : "67aa82ba-67bb-4699-8176-5f572048352b",
//                        "courtRoom" : "c5f066ff-0aff-3dde-8bd9-40a587d3c58e"
//                } ]
//            } ]
//        } ]
//

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
        Pattern bodyPattern = Pattern.compile("^(N?\\d{1,2}M)?(N?\\d{1,2}D)?$");
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

    private static ZoneId getZoneId() {
        return ZoneId.systemDefault();
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

    public CourtHouse getMockCourtHouse() {
//        {
//            "courtHouseType" : "magistrate",
//            "courtHouseCode" : "B01IX00",
//            "courtHouseName" : "Westminster Magistrates' Court",
//            "address" : {
//            "address1" : "181 Marylebone Road",
//                "address2" : "London",
//                "postalCode" : "NW1 5BR",
//                "country" : "UK"
//        },
//            "courtRoom" : [ {
//            "courtRoomId" : 2975,
//                "courtRoomName" : "Courtroom 01"
//        } ]
//        }
        return new CourtHouse(
            CourtHouseType.MAGISTRATE,
            "B01IX00",
            "Westminster Magistrates' Court",
            new Address(
                "181 Marylebone Road",
                "London",
                null,
                null,
                "NW1 5BR",
                "UK"
            ),
            List.of(new CourtRoom(2975, "Courtroom 01"))
        );
    }
}
