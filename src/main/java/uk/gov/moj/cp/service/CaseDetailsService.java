package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto.CourtSittingDto;
import uk.gov.moj.cp.metrics.TrackMyCaseMetricsService;
import uk.gov.moj.cp.model.HearingType;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.parse;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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

        String courtHouseAndRoomIds = getCourtHouseAndRoomIds(caseDetails);
        String weekCommencingIds = getWeekCommencingIds(caseDetails);

        log.atInfo().log("caseUrn : {} -> Received CourtHouse Id and courtRoomId :{} and WeekCommencing CourtHouse Ids : {} ",
                         caseUrn, courtHouseAndRoomIds, weekCommencingIds);
        trackMyCaseMetricsService.incrementCaseDetailsCount(caseUrn);
        return caseDetails;

    }

    private static String getWeekCommencingIds(CaseDetailsDto caseDetails) {
        String weekCommencingIds = caseDetails.courtSchedule().stream()
            .flatMap(a -> a.hearings().stream()
                .filter(b -> nonNull(b.weekCommencing()) && nonNull(b.weekCommencing().courtHouse()))
                .map(b -> b.weekCommencing().courtHouse().courtHouseId())
            ).collect(Collectors.joining("  "));
        return weekCommencingIds;
    }

    private static String getCourtHouseAndRoomIds(CaseDetailsDto caseDetails) {
        String courtHouseAndRoomIds = caseDetails.courtSchedule().stream()
            .flatMap(a -> a.hearings().stream()
                .filter(d-> d.courtSittings() !=null && !d.courtSittings().isEmpty())
                .flatMap(b -> b.courtSittings().stream()
                    .map(c -> c.courtHouse().courtHouseId() + ":" + c.courtHouse().courtRoomId())))
            .collect(Collectors.joining("  "));
        return courtHouseAndRoomIds;
    }

    private static Comparator<CaseDetailsHearingDto> getCaseDetailsHearingDtoComparator() {
        return Comparator.comparing(
                (CaseDetailsHearingDto h) -> getEarliestDate(h),
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing((CaseDetailsHearingDto hearingDto) -> hasSittingStart(hearingDto) ? 0 : 1) // Prioritize hearings with sittingStart over weekCommencing only
            .thenComparingInt((CaseDetailsHearingDto h) ->
                                  HearingType.TRIAL.getValue().equalsIgnoreCase(h.hearingType()) ? 0 : 1
            );
    }

    private static boolean hasSittingStart(CaseDetailsHearingDto hearingDto) {
        return nonNull(hearingDto.courtSittings())
            && !hearingDto.courtSittings().isEmpty()
            && hearingDto.courtSittings().stream()
                .anyMatch(s -> nonNull(s.sittingStart()) && !s.sittingStart().isEmpty());
    }


    private static LocalDate getEarliestDate(CaseDetailsHearingDto hearingDto) {
        LocalDate earliestSittingDate = null;
        LocalDate weekCommencingStartDate = null;
        LocalDate weekCommencingEndDate = null;

        earliestSittingDate = getEarliestSittingDate(hearingDto, earliestSittingDate);

        try {
            weekCommencingStartDate = Optional.ofNullable(hearingDto.weekCommencing())
                .map(weekCommencing -> LocalDate.parse(weekCommencing.startDate()))
                .orElse(null);
            weekCommencingEndDate = Optional.ofNullable(hearingDto.weekCommencing())
                .map(weekCommencing -> LocalDate.parse(weekCommencing.endDate()))
                .orElse(null);
        } catch (Exception exception){
                // Ignore parsing errors
        }

        // Return the earliest date, or null if both are null
        if (nonNull(earliestSittingDate) && nonNull(weekCommencingStartDate)) {
            return earliestSittingDate.isBefore(weekCommencingStartDate) ||
                earliestSittingDate.equals(weekCommencingStartDate) ||
                (earliestSittingDate.isAfter(weekCommencingStartDate) && earliestSittingDate.isBefore(weekCommencingEndDate))
                ? earliestSittingDate : weekCommencingStartDate;
        } else if (nonNull(earliestSittingDate)) {
            return earliestSittingDate;
        } else {
            return weekCommencingEndDate;
        }
    }

    private static LocalDate getEarliestSittingDate(CaseDetailsHearingDto hearingDto, LocalDate earliestSittingDate) {
        if (nonNull(hearingDto.courtSittings()) && !hearingDto.courtSittings().isEmpty()) {
            Optional<LocalDate> sittingDate = hearingDto.courtSittings().stream()
                .filter(s -> nonNull(s.sittingStart()) )
                .map(s -> {
                    try {
                        return parse(s.sittingStart()).toLocalDate();
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
        return earliestSittingDate;
    }

    private CaseDetailsHearingDto getHearingDetails(String accessToken, CourtScheduleDto.HearingDto hearing) {

        boolean hasValidWeekCommencingDate = false;
        CaseDetailsHearingDto.WeekCommencing weekCommencing = null;
        List<CaseDetailsCourtSittingDto> mappedSittings = null;

        if (isNull(hearing) || !isTrailOrSentenceHearing(hearing.hearingType())){
            return null;
        }

        if(Optional.ofNullable(hearing.weekCommencingDto()).isPresent()) {
            weekCommencing = getWeekCommencing(accessToken, hearing);
            if (weekCommencing == null) return null;
        } else {
            mappedSittings = getCaseDetailsCourtSittingDtos(accessToken, hearing);
            if (mappedSittings == null) return null;
        }

        return new CaseDetailsHearingDto(
            mappedSittings,
            hearing.hearingId(),
            hearing.hearingType(),
            hearing.hearingDescription(),
            hearing.listNote(),
            weekCommencing
        );
    }

    private CaseDetailsHearingDto.WeekCommencing getWeekCommencing(String accessToken, CourtScheduleDto.HearingDto hearing) {
        boolean hasValidWeekCommencingDate;
        CaseDetailsHearingDto.WeekCommencing weekCommencing;
        CourtScheduleDto.HearingDto.WeekCommencingDto wc = hearing.weekCommencingDto();
        // Validate that either start date or end date is not in the past (hearing is current or future)
        hasValidWeekCommencingDate = validateWeekCommencingDateNotInPast(wc.startDate())
            || validateWeekCommencingDateNotInPast(wc.endDate());
        if (!hasValidWeekCommencingDate){
            return null;
        }
        final CourtHouseDto courtHouseDto = courtHouseService.getCourtHouseById(
            accessToken,
            wc.courtHouse(),
            null
        );

        weekCommencing = new CaseDetailsHearingDto.WeekCommencing(
           wc.startDate(),
           wc.endDate(),
           wc.durationInWeeks(),
           courtHouseDto
       );
        return weekCommencing;
    }

    private List<CaseDetailsCourtSittingDto> getCaseDetailsCourtSittingDtos(String accessToken, CourtScheduleDto.HearingDto hearing) {
        List<CaseDetailsCourtSittingDto> mappedSittings;
        final List<CourtSittingDto> sittings = hearing.courtSittingDtos();
        final boolean hasAnyCurrentOrFutureSitting = (nonNull(sittings) && !sittings.isEmpty())
            && sittings.stream()
            .anyMatch(s -> validateSittingDateNotInPast(s.sittingStart()));

        if (!hasAnyCurrentOrFutureSitting ) {
            return null;
        }

        mappedSittings = sittings.stream()
            .map(s -> getHearingSchedule(accessToken, s))
            .toList();
        return mappedSittings;
    }


    private CaseDetailsCourtSittingDto getHearingSchedule(
        String accessToken, CourtSittingDto courtSitting) {
        return new CaseDetailsCourtSittingDto(
            courtSitting.judiciaryId(),
            courtSitting.sittingStart(),
            courtSitting.sittingEnd(),
            courtHouseService.getCourtHouseById(accessToken, courtSitting.courtHouse(), courtSitting.courtRoom())
        );
    }

    private boolean validateSittingDateNotInPast(String courtSittingStartDate) {
        if (Optional.ofNullable(courtSittingStartDate).isPresent()) {
            LocalDate sittingDate = parse(courtSittingStartDate).toLocalDate();
            return !sittingDate.isBefore(LocalDate.now());
        }
        return false;
    }

    private boolean validateWeekCommencingDateNotInPast(String weekCommencingStartDate) {
        if (nonNull(weekCommencingStartDate) && !weekCommencingStartDate.isEmpty()) {
            try {
                LocalDate weekCommencingDate = LocalDate.parse(weekCommencingStartDate);
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

