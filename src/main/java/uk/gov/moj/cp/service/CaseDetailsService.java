package uk.gov.moj.cp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.outbound.CaseDetailsDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsCourtScheduleDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsWeekCommencingDto;
import uk.gov.moj.cp.dto.outbound.CourtHouseDto;
import uk.gov.moj.cp.dto.inbound.CourtScheduleDto;
import uk.gov.moj.cp.dto.inbound.HearingDto;
import uk.gov.moj.cp.dto.inbound.CourtSittingDto;
import uk.gov.moj.cp.dto.inbound.WeekCommencingDto;
import uk.gov.moj.cp.metrics.TrackMyCaseMetricsService;
import uk.gov.moj.cp.model.HearingType;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.time.LocalDateTime.parse;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaseDetailsService {

    private final CourtScheduleService courtScheduleService;
    private final CourtHouseService courtHouseService;
    private final OAuthTokenService oauthTokenService;
    private final TrackMyCaseMetricsService trackMyCaseMetricsService;

    public CaseDetailsDto getCaseDetailsByCaseUrn(final String caseUrn) {
        String accessToken = oauthTokenService.getJwtToken();
        List<CourtScheduleDto> courtSchedule = courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn);

        List<CaseDetailsCourtScheduleDto> caseDetailsCourtSchedules = courtSchedule.stream()
            .map(schedule -> {
                List<CaseDetailsHearingDto> nextHearings = schedule.getHearings().stream()
                    .map(this::getHearingDetails)
                    .filter(Objects::nonNull)
                    .min(getCaseDetailsHearingDtoComparator())
                    .map(h -> enrichHearingWithCourtDetails(caseUrn, accessToken, h))
                    .stream()
                    .toList();
                return CaseDetailsCourtScheduleDto.builder()
                    .hearings(nextHearings)
                    .build();

            })
            .toList();

        trackMyCaseMetricsService.incrementCaseDetailsCount(caseUrn);

        return CaseDetailsDto.builder()
            .caseUrn(caseUrn)
            .courtSchedules(caseDetailsCourtSchedules)
            .build();
    }


    private CaseDetailsHearingDto getHearingDetails(final HearingDto hearing) {
        CaseDetailsWeekCommencingDto weekCommencing = null;
        List<CaseDetailsCourtSittingDto> courtSittings = null;

        if (isNull(hearing) || !isTrailOrSentenceHearing(hearing.getHearingType())) {
            return null;
        }

        if (Optional.ofNullable(hearing.getWeekCommencing()).isPresent()) {
            weekCommencing = getWeekCommencing(hearing);
            if (isNull(weekCommencing)) {
                return null;
            }
        } else {
            courtSittings = getCourtSittings(hearing);
            if (isNull(courtSittings)) {
                return null;
            }
        }

        return CaseDetailsHearingDto.builder()
            .hearingId(hearing.getHearingId())
            .hearingType( hearing.getHearingType())
            .hearingDescription(hearing.getHearingDescription())
            .listNote(hearing.getListNote())
            .courtSittings(courtSittings)
            .weekCommencing(weekCommencing)
            .build();
    }

    private static Comparator<CaseDetailsHearingDto> getCaseDetailsHearingDtoComparator() {
        return Comparator.comparing(
                CaseDetailsService::getEarliestHearingDate,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing((CaseDetailsHearingDto dto) -> hasFixedDateHearing(dto) ? 0 : 1)
            .thenComparingInt((CaseDetailsHearingDto dto) ->
                                  HearingType.TRIAL.getValue().equalsIgnoreCase(dto.getHearingType()) ? 0 : 1
            );
    }

    private static boolean hasFixedDateHearing(final CaseDetailsHearingDto hearingDto) {
        return nonNull(hearingDto.getCourtSittings())
            && !hearingDto.getCourtSittings().isEmpty()
            && hearingDto.getCourtSittings().stream()
            .anyMatch(s -> nonNull(s.getSittingStart()) && !s.getSittingStart().isEmpty());
    }

    private static LocalDate getEarliestHearingDate(final CaseDetailsHearingDto hearingDto) {
        LocalDate weekCommencingStartDate = null;
        LocalDate weekCommencingEndDate = null;

        LocalDate earliestSittingDate = getEarliestCourtSittingDate(hearingDto);
        try {
            weekCommencingStartDate = Optional.ofNullable(hearingDto.getWeekCommencing())
                .map(weekCommencing -> LocalDate.parse(weekCommencing.getStartDate()))
                .orElse(null);
            weekCommencingEndDate = Optional.ofNullable(hearingDto.getWeekCommencing())
                .map(weekCommencing -> LocalDate.parse(weekCommencing.getEndDate()))
                .orElse(null);
        } catch (Exception e) {
            log.atError().log("parsing error for hearing {} : {}", hearingDto.getHearingId(), e.getMessage());
        }

        if (nonNull(earliestSittingDate) && nonNull(weekCommencingStartDate)) {
            return earliestSittingDate.isBefore(weekCommencingStartDate) ||
                earliestSittingDate.equals(weekCommencingStartDate) ||
                (
                    earliestSittingDate.isAfter(weekCommencingStartDate)
                        && earliestSittingDate.isBefore(weekCommencingEndDate)
                )
                ? earliestSittingDate : weekCommencingStartDate;
        } else if (nonNull(earliestSittingDate)) {
            return earliestSittingDate;
        } else {
            return weekCommencingEndDate;
        }
    }

    private static LocalDate getEarliestCourtSittingDate(final CaseDetailsHearingDto hearingDto) {
        LocalDate earliestSittingDate = null;
        if (nonNull(hearingDto.getCourtSittings()) && !hearingDto.getCourtSittings().isEmpty()) {
            Optional<LocalDate> sittingDate = hearingDto.getCourtSittings()
                .stream()
                .filter(s -> nonNull(s.getSittingStart()))
                .map(s -> {
                    try {
                        return parse(s.getSittingStart()).toLocalDate();
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

    private CaseDetailsWeekCommencingDto getWeekCommencing(final HearingDto hearing) {
        final WeekCommencingDto weekCommencingDto = hearing.getWeekCommencing();
        final boolean hasValidWeekCommencingDate = validateWeekCommencingDateNotInPast(weekCommencingDto.getStartDate())
            || validateWeekCommencingDateNotInPast(weekCommencingDto.getEndDate());
        if (!hasValidWeekCommencingDate) {
            return null;
        }

        final CourtHouseDto courtHouseDto = CourtHouseDto.builder()
            .courtHouseId(weekCommencingDto.getCourtHouse())
            .build();

        return CaseDetailsWeekCommencingDto.builder()
            .startDate(weekCommencingDto.getStartDate())
            .endDate(weekCommencingDto.getEndDate())
            .durationInWeeks(weekCommencingDto.getDurationInWeeks())
            .courtHouse(courtHouseDto)
            .build();

    }

    private List<CaseDetailsCourtSittingDto> getCourtSittings(final HearingDto hearing) {

        final List<CourtSittingDto> sittings = hearing.getCourtSittings();
        final boolean hasAnyCurrentOrFutureSitting = (nonNull(sittings) && !sittings.isEmpty())
            && sittings.stream()
            .anyMatch(s -> validateSittingDateNotInPast(s.getSittingStart()));

        if (!hasAnyCurrentOrFutureSitting) {
            return null;
        }

        return sittings.stream()
            .map(this::populateCourtSittings)
            .toList();
    }

    private CaseDetailsCourtSittingDto populateCourtSittings(final CourtSittingDto courtSitting) {
        final CourtHouseDto courtHouseDto = CourtHouseDto.builder()
            .courtHouseId(courtSitting.getCourtHouse())
            .courtRoomId(courtSitting.getCourtRoom())
            .build();

        return CaseDetailsCourtSittingDto.builder()
            .judiciaryId(courtSitting.getJudiciaryId())
            .sittingStart(courtSitting.getSittingStart())
            .sittingEnd(courtSitting.getSittingEnd())
            .courtHouse(courtHouseDto)
            .build();
    }

    private boolean validateSittingDateNotInPast(final String courtSittingStartDate) {
        if (Optional.ofNullable(courtSittingStartDate).isPresent()) {
            LocalDate sittingDate = parse(courtSittingStartDate).toLocalDate();
            return !sittingDate.isBefore(LocalDate.now());
        }
        return false;
    }

    private boolean validateWeekCommencingDateNotInPast(final String weekCommencingStartDate) {
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

    private boolean isTrailOrSentenceHearing(final String hearingType) {
        return HearingType.TRIAL.getValue().equalsIgnoreCase(hearingType)
            || HearingType.SENTENCE.getValue().equalsIgnoreCase(hearingType);
    }

    private CaseDetailsHearingDto enrichHearingWithCourtDetails(final String caseUrn, final String accessToken, final CaseDetailsHearingDto hearing) {
        CaseDetailsWeekCommencingDto enrichedWeekCommencing = enrichWeekCommencingWithCourtDetails(accessToken, hearing.getWeekCommencing());

        List<CaseDetailsCourtSittingDto> enrichedCourtSittings =
            (isNull(enrichedWeekCommencing))
                ? enrichCourtSittingsWithCourtDetails(accessToken, hearing.getCourtSittings())
                : null;

        if (nonNull(enrichedWeekCommencing)) {
            log.atInfo().log(
                "caseUrn -{} : hearingId (W/C) - {} : CourtHouse Id - {} ",
                caseUrn,
                hearing.getHearingId(),
                enrichedWeekCommencing.getCourtHouse().getCourtHouseId()
            );
        } else {
            log.atInfo().log(
                "caseUrn -{} : hearingId - {} : CourtHouse Id - {} :  CourtRoom Id : {}",
                caseUrn,
                hearing.getHearingId(),
                enrichedCourtSittings.getFirst().getCourtHouse().getCourtHouseId(),
                enrichedCourtSittings.getFirst().getCourtHouse().getCourtRoomId()
            );
        }

        return  CaseDetailsHearingDto.builder()
            .hearingId(hearing.getHearingId())
            .hearingType(hearing.getHearingType())
            .hearingDescription( hearing.getHearingDescription())
            .listNote( hearing.getListNote())
            .courtSittings(enrichedCourtSittings)
            .weekCommencing(enrichedWeekCommencing)
            .build();
    }

    private CaseDetailsWeekCommencingDto enrichWeekCommencingWithCourtDetails(final String accessToken, final CaseDetailsWeekCommencingDto weekCommencing) {
        if (isNull(weekCommencing))
            return null;

        final CourtHouseDto courtHouseDto = courtHouseService.getCourtHouseById(
            accessToken,
            weekCommencing.getCourtHouse().getCourtHouseId(),
            null
        );

        return CaseDetailsWeekCommencingDto.builder()
            .startDate(weekCommencing.getStartDate())
            .endDate(weekCommencing.getEndDate())
            .durationInWeeks(weekCommencing.getDurationInWeeks())
            .courtHouse(courtHouseDto)
            .build();
    }

    private List<CaseDetailsCourtSittingDto> enrichCourtSittingsWithCourtDetails(final String accessToken, final List<CaseDetailsCourtSittingDto> courtSittings
    ) {
        if (isNull(courtSittings))
             return null;

        final String courtHouseId = courtSittings.getFirst().getCourtHouse().getCourtHouseId();
        final String courtRoomId = courtSittings.getFirst().getCourtHouse().getCourtRoomId();
        final CourtHouseDto courtHouseDto = courtHouseService.getCourtHouseById(accessToken, courtHouseId, courtRoomId);

        return courtSittings.stream()
            .map(cs ->
                     CaseDetailsCourtSittingDto.builder()
                         .judiciaryId(cs.getJudiciaryId())
                         .sittingStart(cs.getSittingStart())
                         .sittingEnd(cs.getSittingEnd())
                         .courtHouse(courtHouseDto)
                         .build()
            )
            .toList();
    }
}

