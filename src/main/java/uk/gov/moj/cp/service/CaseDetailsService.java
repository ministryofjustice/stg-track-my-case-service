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
                        .map(t -> getHearingDetails(accessToken, t, caseUrn))
                        .filter(Objects::nonNull)
                        .sorted(
                            getCaseDetailsHearingDtoComparator()
                        )
                        .toList()
                ))
                .toList()
        );

        String courtHouseAndRoomIds = getCourtHouseAndCortRoomIdsForFixedDateHearing(caseDetails);
        String weekCommencingIds = getCourtHouseIdsForWeekCommencingHearing(caseDetails);

        log.atInfo().log("caseUrn : {} -> Received CourtHouse Id and courtRoomId :{} and WeekCommencing CourtHouse Ids : {} ",
                         caseUrn, courtHouseAndRoomIds, weekCommencingIds);

        trackMyCaseMetricsService.incrementCaseDetailsCount(caseUrn);
        return caseDetails;

    }

    private CaseDetailsHearingDto getHearingDetails(String accessToken, CourtScheduleDto.HearingDto hearing, String caseUrn) {

        CaseDetailsHearingDto.WeekCommencing weekCommencing = null;
        List<CaseDetailsCourtSittingDto> courtSittings = null;

        if (isNull(hearing) || !isTrailOrSentenceHearing(hearing.hearingType())){
            return null;
        }

        if(Optional.ofNullable(hearing.weekCommencingDto()).isPresent()) {
            weekCommencing = getWeekCommencing(accessToken, hearing);
            if (isNull(weekCommencing)){
                return null;
            }
        } else {
            courtSittings = getCourtSittings(accessToken, hearing);
            if (isNull(courtSittings)) {
                return null;
            }
        }

        final List<CaseDetailsCourtSittingDto> mappedSittings = sittings.stream()
            .map(s -> getHearingSchedule(accessToken, s, caseUrn))
            .toList();

        return new CaseDetailsHearingDto(
            courtSittings,
            hearing.hearingId(),
            hearing.hearingType(),
            hearing.hearingDescription(),
            hearing.listNote(),
            weekCommencing
        );
    }

    private static Comparator<CaseDetailsHearingDto> getCaseDetailsHearingDtoComparator() {
        return Comparator.comparing(
                CaseDetailsService::getEarliestHearingDate,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing((CaseDetailsHearingDto hearingDto) -> hasFixedDateHearing(hearingDto) ? 0 : 1)
            .thenComparingInt((CaseDetailsHearingDto h) ->
                                  HearingType.TRIAL.getValue().equalsIgnoreCase(h.hearingType()) ? 0 : 1
            );
    }

    private static boolean hasFixedDateHearing(CaseDetailsHearingDto hearingDto) {
        return nonNull(hearingDto.courtSittings())
            && !hearingDto.courtSittings().isEmpty()
            && hearingDto.courtSittings().stream()
                .anyMatch(s -> nonNull(s.sittingStart()) && !s.sittingStart().isEmpty());
    }


    private static LocalDate getEarliestHearingDate(CaseDetailsHearingDto hearingDto) {
        LocalDate weekCommencingStartDate = null;
        LocalDate weekCommencingEndDate = null;

        LocalDate earliestSittingDate = getEarliestCourtSittingDate(hearingDto);
        try {
            weekCommencingStartDate = Optional.ofNullable(hearingDto.weekCommencing())
                .map(weekCommencing -> LocalDate.parse(weekCommencing.startDate()))
                .orElse(null);
            weekCommencingEndDate = Optional.ofNullable(hearingDto.weekCommencing())
                .map(weekCommencing -> LocalDate.parse(weekCommencing.endDate()))
                .orElse(null);
        } catch (Exception exception){
            log.atError().log("parsing error for hearing {} : {}", hearingDto.hearingId(), exception.getMessage());
        }

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

    private static LocalDate getEarliestCourtSittingDate(CaseDetailsHearingDto hearingDto) {
        LocalDate earliestSittingDate = null;
        if (nonNull(hearingDto.courtSittings()) && !hearingDto.courtSittings().isEmpty()) {
             Optional <LocalDate> sittingDate = hearingDto.courtSittings().stream()
                .filter(s -> nonNull(s.sittingStart()))
                .map(s -> {
                    try {
                        return parse(s.sittingStart()).toLocalDate();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder());

             if(sittingDate.isPresent()){
                 earliestSittingDate = sittingDate.get();
             }
        }
        return earliestSittingDate;
    }


    private CaseDetailsHearingDto.WeekCommencing getWeekCommencing(String accessToken, CourtScheduleDto.HearingDto hearing) {

        CourtScheduleDto.HearingDto.WeekCommencingDto weekCommencingDto = hearing.weekCommencingDto();
        final boolean hasValidWeekCommencingDate = validateWeekCommencingDateNotInPast(weekCommencingDto.startDate())
            || validateWeekCommencingDateNotInPast(weekCommencingDto.endDate());
        if (!hasValidWeekCommencingDate) {
            return null;
        }
        final CourtHouseDto courtHouseDto = courtHouseService.getCourtHouseById(
            accessToken,
            weekCommencingDto.courtHouse(),
            null
        );
        return new CaseDetailsHearingDto.WeekCommencing(
            weekCommencingDto.startDate(),
            weekCommencingDto.endDate(),
            weekCommencingDto.durationInWeeks(),
            courtHouseDto
        );
    }

    private List<CaseDetailsCourtSittingDto> getCourtSittings(String accessToken, CourtScheduleDto.HearingDto hearing) {

        final List<CourtSittingDto> sittings = hearing.courtSittingDtos();
        final boolean hasAnyCurrentOrFutureSitting = (nonNull(sittings) && !sittings.isEmpty())
            && sittings.stream()
            .anyMatch(s -> validateSittingDateNotInPast(s.sittingStart()));

        if (!hasAnyCurrentOrFutureSitting ) {
            return null;
        }

        return sittings.stream()
            .map(s -> getHearingSchedule(accessToken, s))
            .toList();
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

    private CaseDetailsCourtSittingDto getHearingSchedule(
        String accessToken, CourtScheduleDto.HearingDto.CourtSittingDto sitting, String caseUrn) {
        return new CaseDetailsCourtSittingDto(
            sitting.judiciaryId(),
            sitting.sittingStart(),
            sitting.sittingEnd(),
            courtHouseService.getCourtHouseById(accessToken, caseUrn, sitting.courtHouse(), sitting.courtRoom())
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

    private static String getCourtHouseIdsForWeekCommencingHearing(CaseDetailsDto caseDetails) {
        return caseDetails.courtSchedule().stream()
            .flatMap(a -> a.hearings().stream()
                .filter(b -> nonNull(b.weekCommencing()) && nonNull(b.weekCommencing().courtHouse()))
                .map(b -> b.weekCommencing().courtHouse().courtHouseId())
            ).collect(Collectors.joining("  "));
    }

    private static String getCourtHouseAndCortRoomIdsForFixedDateHearing(CaseDetailsDto caseDetails) {
        return caseDetails.courtSchedule().stream()
            .flatMap(a -> a.hearings().stream()
                .filter(d-> d.courtSittings() !=null && !d.courtSittings().isEmpty())
                .flatMap(b -> b.courtSittings().stream()
                    .map(c -> c.courtHouse().courtHouseId() + ":" + c.courtHouse().courtRoomId())))
            .collect(Collectors.joining("  "));
    }
}

