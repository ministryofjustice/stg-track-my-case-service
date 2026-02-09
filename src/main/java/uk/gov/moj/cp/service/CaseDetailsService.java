package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto.CourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.metrics.TrackMyCaseMetricsService;
import uk.gov.moj.cp.model.HearingType;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Service
@Slf4j
public class CaseDetailsService {

    @Autowired
    private CourtScheduleService courtScheduleService;
    @Autowired
    private CourtHouseService courtHouseService;

    @Autowired
    private OAuthTokenService oauthTokenService;

    @Autowired
    private TrackMyCaseMetricsService trackMyCaseMetricsService;

    public CaseDetailsDto getCaseDetailsByCaseUrn(String caseUrn) {
        String accessToken = oauthTokenService.getJwtToken();
        List<CourtScheduleDto> courtSchedule = courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn);

        CaseDetailsDto caseDetails = new CaseDetailsDto(
            caseUrn,
            courtSchedule.stream()
                .map(schedule -> new CaseDetailsDto.CaseDetailsCourtScheduleDto(
                    schedule.hearingDtos().stream()
                        .map(t -> getHearingDetails(accessToken, t))
                        .filter(Objects::nonNull)
                        .sorted(
                            getCaseDetailsHearingDtoComparator()
                        )
                        .toList()
                ))
                .toList()
        );

        String courtHouseAndRoomIds = caseDetails.courtSchedule().stream()
            .flatMap(a -> a.hearings().stream()
                .flatMap(b -> b.courtSittings().stream()
                    .map(c -> c.courtHouse().courtHouseId() + ":" + c.courtHouse().courtRoomId())))
            .collect(Collectors.joining("  "));

        log.atInfo().log("caseUrn : {} -> Received CourtHouse Id and courtRoomId :{}",
                         caseUrn, courtHouseAndRoomIds);
        trackMyCaseMetricsService.incrementCaseDetailsCount(caseUrn);
        return caseDetails;

    }

    private static Comparator<CaseDetailsHearingDto> getCaseDetailsHearingDtoComparator() {
        return Comparator.comparing(
                (CaseDetailsHearingDto h) -> getEarliestDate(h),
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparingInt((CaseDetailsHearingDto h) ->
                                  HearingType.TRIAL.getValue().equalsIgnoreCase(h.hearingType()) ? 0 : 1
            );
    }

    private static LocalDate getEarliestDate(CaseDetailsHearingDto h) {
        LocalDate earliestSittingDate = null;
        LocalDate weekCommencingStartDate = null;

        // Get earliest sittingStart date if present
        if (h.courtSittings() != null && !h.courtSittings().isEmpty()) {
            Optional<LocalDate> sittingDate = h.courtSittings().stream()
                .filter(Objects::nonNull)
                .filter(s -> s.sittingStart() != null && !s.sittingStart().equals("N/A"))
                .map(s -> {
                    try {
                        return LocalDateTime.parse(s.sittingStart()).toLocalDate();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder());

            if (sittingDate.isPresent()) {
                earliestSittingDate = sittingDate.get();
            }
        }

        if (h.weekCommencingStartDate() != null && !h.weekCommencingStartDate().isEmpty()) {
            try {
                weekCommencingStartDate = LocalDate.parse(h.weekCommencingStartDate());
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Return the earliest date, or null if both are null
        if (earliestSittingDate != null && weekCommencingStartDate != null) {
            return earliestSittingDate.isBefore(weekCommencingStartDate) ||
                earliestSittingDate.equals(weekCommencingStartDate) ? earliestSittingDate : weekCommencingStartDate;
        } else if (earliestSittingDate != null) {
            return earliestSittingDate;
        } else {
            return weekCommencingStartDate;
        }
    }

    private CaseDetailsHearingDto getHearingDetails(String accessToken, CourtScheduleDto.HearingDto hearing) {

        if (isNull(hearing) || !isTrailOrSentenceHearing(hearing.hearingType())){
            return null;
        }

        final List<CourtSittingDto> sittings = hearing.courtSittingDtos();

        final boolean hasInValidSittingDates = sittings.stream()
            .anyMatch(s -> isNull(s) || sittings.isEmpty());
        final boolean hasAnyCurrentOrFutureSitting = sittings.stream()
            .anyMatch(s -> validateSittingDateNotInPast(s.sittingStart()));

        final boolean hasValidWeekCommencingDate = validateWeekCommencingDateNotInPast(hearing.weekCommencingStartDate());

        if (!hasAnyCurrentOrFutureSitting && !hasValidWeekCommencingDate && hasInValidSittingDates) {
            return null;
        }

        final List<CaseDetailsCourtSittingDto> mappedSittings = sittings.stream()
            .map(s -> getHearingSchedule(accessToken, s))
            .toList();

        return new CaseDetailsHearingDto(
            mappedSittings,
            hearing.hearingId(),
            hearing.hearingType(),
            hearing.hearingDescription(),
            hearing.listNote(),
            hearing.weekCommencingStartDate(),
            hearing.weekCommencingEndDate(),
            hearing.weekCommencingDurationInWeeks()
        );
    }

    private CaseDetailsCourtSittingDto getHearingSchedule(
        String accessToken, CourtScheduleDto.HearingDto.CourtSittingDto sitting) {
        return new CaseDetailsCourtSittingDto(
            sitting.judiciaryId(),
            sitting.sittingStart(),
            sitting.sittingEnd(),
            courtHouseService.getCourtHouseById(accessToken, sitting.courtHouse(), sitting.courtRoom())
        );
    }

    private boolean validateSittingDateNotInPast(String dateTimeString) {
        if (Optional.ofNullable(dateTimeString).isPresent()) {
            LocalDate sittingDate = LocalDateTime.parse(dateTimeString).toLocalDate();
            return !sittingDate.isBefore(LocalDate.now());
        }
        return false;
    }

    private boolean validateWeekCommencingDateNotInPast(String dateString) {
        if (dateString != null && !dateString.isEmpty()) {
            try {
                LocalDate weekCommencingDate = LocalDate.parse(dateString);
                return !weekCommencingDate.isBefore(LocalDate.now());
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private boolean isTrailOrSentenceHearing(final String hearingType){
        return HearingType.TRIAL.getValue().equalsIgnoreCase(hearingType)
            || HearingType.SENTENCE.getValue().equalsIgnoreCase(hearingType);
    }
}

