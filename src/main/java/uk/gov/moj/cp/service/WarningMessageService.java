package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.model.HearingType;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class WarningMessageService {

    public static final String TOMORROW = "TOMORROW";
    public static final String TODAY = "TODAY";
    public static final String SENTENCING_HEARING = "SENTENCING_HEARING";
    public static final String TRIAL_HEARING = "TRIAL_HEARING";
    private Clock clock = Clock.systemDefaultZone();

    public WarningMessageService() {
    }

    public WarningMessageService(Clock clock) {
        this.clock = clock;
    }

    public String getMessage(Optional<String> firstHearingType, List<CaseDetailsCourtSittingDto> sittings) {

        if (firstHearingType.isEmpty() || sittings == null || sittings.isEmpty()) {
            return null;
        }

        String hearingType = firstHearingType.get();

        // Check if hearing is ongoing (current date is between sitting start dates of multiple sittings)
        LocalDate today = LocalDate.now(clock);
        boolean isOngoing = isHearingOngoing(sittings, today);

        if (isOngoing) {
            String hearingTypePrefix = convertHearingTypeToMessageFormat(hearingType);
            if (hearingTypePrefix == null) {
                return null;
            }
            return hearingTypePrefix + "_IS_ONGOING";
        }

        // If not ongoing, use the first sitting for the existing logic
        CaseDetailsCourtSittingDto firstSitting = sittings.getFirst();

        if (firstSitting.sittingStart() == null || firstSitting.sittingStart().equals("N/A")) {
            return null;
        }

        try {
            LocalDateTime sittingStartDateTime = LocalDateTime.parse(firstSitting.sittingStart());
            LocalDate sittingDate = sittingStartDateTime.toLocalDate();

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

            if (TODAY.equals(periodSuffix)) {
                return hearingTypePrefix + "_STARTS_" + TODAY;
            }

            if (TOMORROW.equals(periodSuffix)) {
                return hearingTypePrefix + "_STARTS_" + TOMORROW;
            }
            return hearingTypePrefix + "_STARTS_IN_" + periodSuffix;

        } catch (Exception e) {
            log.atWarn().log("Failed to parse sitting start date: {}", firstSitting.sittingStart(), e);
            return null;
        }
    }

    private boolean isHearingOngoing(List<CaseDetailsCourtSittingDto> sittings, LocalDate today) {
        if (sittings == null || sittings.size() < 2) {
            return false;
        }

        try {
            List<LocalDate> sittingStartDates = sittings.stream()
                .filter(sitting -> sitting.sittingStart() != null && !sitting.sittingStart().equals("N/A"))
                .map(sitting -> {
                    try {
                        LocalDateTime sittingStart = LocalDateTime.parse(sitting.sittingStart());
                        return sittingStart.toLocalDate();
                    } catch (Exception e) {
                        log.atWarn().log("Failed to parse sitting start date: {}", sitting.sittingStart(), e);
                        return null;
                    }
                })
                .filter(date -> date != null)
                .sorted()
                .toList();

            if (sittingStartDates.size() < 2) {
                return false;
            }

            LocalDate earliestSittingDate = sittingStartDates.getFirst();
            LocalDate latestSittingDate = sittingStartDates.getLast();
            return earliestSittingDate.isBefore(today) &&
                    ( (latestSittingDate.isAfter(today)|| latestSittingDate.isEqual(today)) );
        } catch (Exception e) {
            log.atWarn().log("Failed to check if hearing is ongoing", e);
            return false;
        }
    }

    private String convertHearingTypeToMessageFormat(String hearingType) {
        if (HearingType.SENTENCE.getValue().equalsIgnoreCase(hearingType)) {
            return SENTENCING_HEARING;
        } else if (HearingType.TRIAL.getValue().equalsIgnoreCase(hearingType)) {
            return TRIAL_HEARING;
        }
        return null;
    }

    private String formatPeriod(int months, int days) {
        if (months == 0 && days == 0) {
            return TODAY;
        }

        if (months == 0 && days == 1) {
            return TOMORROW;
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

