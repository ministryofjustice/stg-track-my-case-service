package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.model.HearingType;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class WarningMessageService {

    private Clock clock = Clock.systemDefaultZone();

    public WarningMessageService() {
    }

    public WarningMessageService(Clock clock) {
        this.clock = clock;
    }

    public String getMessage(Optional<String> firstHearingType, Optional<CaseDetailsCourtSittingDto> firstSitting) {

        if (firstHearingType.isEmpty() || firstSitting.isEmpty()) {
            return null;
        }

        String hearingType = firstHearingType.get();
        CaseDetailsCourtSittingDto sitting = firstSitting.get();

        if (sitting.sittingStart() == null || sitting.sittingStart().equals("N/A")) {
            return null;
        }

        try {
            LocalDateTime sittingStartDateTime = LocalDateTime.parse(sitting.sittingStart());
            LocalDate sittingDate = sittingStartDateTime.toLocalDate();
            LocalDate today = LocalDate.now(clock);

            java.time.Period period = java.time.Period.between(today, sittingDate);

            int totalMonths = period.getYears() * 12 + period.getMonths();
            int days = period.getDays();

            if (sittingDate.isBefore(today)) {
                return null;
            }

            String hearingTypePrefix = convertHearingTypeToMessageFormat(hearingType);
            if (hearingTypePrefix == null) {
                return null;
            }

            String periodSuffix = formatPeriod(totalMonths, days);
            if (periodSuffix == null) {
                return null;
            }

            if ("TODAY".equals(periodSuffix)) {
                return hearingTypePrefix + "_STARTS_TODAY";
            }

            if ("TOMRROW".equals(periodSuffix)) {
                return hearingTypePrefix + "_STARTS_TOMRROW";
            }
            return hearingTypePrefix + "_STARTS_IN_" + periodSuffix;

        } catch (Exception e) {
            log.atWarn().log("Failed to parse sitting start date: {}", sitting.sittingStart(), e);
            return null;
        }
    }

    private String convertHearingTypeToMessageFormat(String hearingType) {
        if (HearingType.SENTENCE.getValue().equalsIgnoreCase(hearingType)) {
            return "SENTENCING_HEARING";
        } else if (HearingType.TRIAL.getValue().equalsIgnoreCase(hearingType)) {
            return "TRIAL_HEARING";
        }
        return null;
    }

    private String formatPeriod(int months, int days) {
        if (months == 0 && days == 0) {
            return "TODAY";
        }

        if (months == 0 && days == 1) {
            return "TOMRROW";
        }

        StringBuilder period = new StringBuilder();
        if (months > 0) {
            period.append(numberToWord(months));
            period.append(months == 1 ? "_MONTH" : "_MONTHS");
        }

        if (days > 0) {
            if (months > 0) {
                period.append("_");
            }
            period.append(numberToWord(days));
            period.append(days == 1 ? "_DAY" : "_DAYS");
        }
        return period.toString();
    }

    private String numberToWord(int number) {
        if (number < 0 || number > 99) {
            return String.valueOf(number);
        }

        String[] ones = {
            "ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
            "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN",
            "SEVENTEEN", "EIGHTEEN", "NINETEEN"
        };

        String[] tens = {
            "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
        };

        if (number < 20) {
            return ones[number];
        }

        int tensDigit = number / 10;
        int onesDigit = number % 10;

        if (onesDigit == 0) {
            return tens[tensDigit];
        } else {
            return tens[tensDigit] + "_" + ones[onesDigit];
        }
    }
}

