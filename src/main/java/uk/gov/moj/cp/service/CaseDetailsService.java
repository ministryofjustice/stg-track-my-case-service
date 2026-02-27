package uk.gov.moj.cp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.WeekCommencing;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto.CourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto.HearingDto.WeekCommencingDto;
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

        CaseDetailsDto caseDetails = new CaseDetailsDto(
            caseUrn,
            courtSchedule.stream()
                .map(schedule -> {
                    List<CaseDetailsHearingDto> nextHearing = schedule.hearingDtos().stream()
                            .map(this::getHearingDetails)
                            .filter(Objects::nonNull)
                            .min(getCaseDetailsHearingDtoComparator())
                            .map(h -> enrichHearingWithCourtDetails(caseUrn, accessToken, h))
                            .stream()
                            .toList();

                    return new CaseDetailsCourtScheduleDto(nextHearing);
                })
                .toList()
        );

        trackMyCaseMetricsService.incrementCaseDetailsCount(caseUrn);
        return caseDetails;
    }


    private CaseDetailsHearingDto getHearingDetails(final HearingDto hearing) {
        WeekCommencing weekCommencing = null;
        List<CaseDetailsCourtSittingDto> courtSittings = null;

        if (isNull(hearing) || !isTrailOrSentenceHearing(hearing.hearingType())) {
            return null;
        }

        if (Optional.ofNullable(hearing.weekCommencingDto()).isPresent()) {
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
            .thenComparing((CaseDetailsHearingDto dto) -> hasFixedDateHearing(dto) ? 0 : 1)
            .thenComparingInt((CaseDetailsHearingDto dto) ->
                                  HearingType.TRIAL.getValue().equalsIgnoreCase(dto.hearingType()) ? 0 : 1
            );
    }

    private static boolean hasFixedDateHearing(final CaseDetailsHearingDto hearingDto) {
        return nonNull(hearingDto.courtSittings())
            && !hearingDto.courtSittings().isEmpty()
            && hearingDto.courtSittings().stream()
            .anyMatch(s -> nonNull(s.sittingStart()) && !s.sittingStart().isEmpty());
    }

    private static LocalDate getEarliestHearingDate(final CaseDetailsHearingDto hearingDto) {
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
        } catch (Exception e) {
            log.atError().log("parsing error for hearing {} : {}", hearingDto.hearingId(), e.getMessage());
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
        if (nonNull(hearingDto.courtSittings()) && !hearingDto.courtSittings().isEmpty()) {
            Optional<LocalDate> sittingDate = hearingDto.courtSittings()
                .stream()
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

            if (sittingDate.isPresent()) {
                earliestSittingDate = sittingDate.get();
            }
        }
        return earliestSittingDate;
    }

    private WeekCommencing getWeekCommencing(final HearingDto hearing) {
        final WeekCommencingDto weekCommencingDto = hearing.weekCommencingDto();
        final boolean hasValidWeekCommencingDate = validateWeekCommencingDateNotInPast(weekCommencingDto.startDate())
            || validateWeekCommencingDateNotInPast(weekCommencingDto.endDate());
        if (!hasValidWeekCommencingDate) {
            return null;
        }
        CourtHouseDto courtHouseDto = CourtHouseDto.withCourtHouseId(weekCommencingDto.courtHouse());
        return new WeekCommencing(
            weekCommencingDto.startDate(),
            weekCommencingDto.endDate(),
            weekCommencingDto.durationInWeeks(),
            courtHouseDto
        );
    }

    private List<CaseDetailsCourtSittingDto> getCourtSittings(final HearingDto hearing) {

        final List<CourtSittingDto> sittings = hearing.courtSittingDtos();
        final boolean hasAnyCurrentOrFutureSitting = (nonNull(sittings) && !sittings.isEmpty())
            && sittings.stream()
            .anyMatch(s -> validateSittingDateNotInPast(s.sittingStart()));

        if (!hasAnyCurrentOrFutureSitting) {
            return null;
        }

        return sittings.stream()
            .map(this::populateCourtSittings)
            .toList();
    }

    private CaseDetailsCourtSittingDto populateCourtSittings(final CourtSittingDto courtSitting) {
        CourtHouseDto courtHouseDto = CourtHouseDto.withCourtHouseIdAndCourtRoomId(courtSitting.courtHouse(), courtSitting.courtRoom());
        return new CaseDetailsCourtSittingDto(
            courtSitting.judiciaryId(),
            courtSitting.sittingStart(),
            courtSitting.sittingEnd(),
            courtHouseDto
        );
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
        WeekCommencing enrichedWeekCommencing = enrichWeekCommencingWithCourtDetails(accessToken, hearing.weekCommencing());

        List<CaseDetailsCourtSittingDto> enrichedCourtSittings =
            (isNull(enrichedWeekCommencing))
                ? enrichCourtSittingsWithCourtDetails(accessToken, hearing.courtSittings())
                : null;

        if (nonNull(enrichedWeekCommencing)) {
            log.atInfo().log(
                "caseUrn -{} : hearingId (W/C) - {} : CourtHouse Id - {} ",
                caseUrn,
                hearing.hearingId(),
                enrichedWeekCommencing.courtHouse().courtHouseId()
            );
        } else {
            log.atInfo().log(
                "caseUrn -{} : hearingId - {} : CourtHouse Id - {} :  CourtRoom Id : {}",
                caseUrn,
                hearing.hearingId(),
                enrichedCourtSittings.getFirst().courtHouse().courtHouseId(),
                enrichedCourtSittings.getFirst().courtHouse().courtRoomId()
            );
        }

        return new CaseDetailsHearingDto(
            enrichedCourtSittings,
            hearing.hearingId(),
            hearing.hearingType(),
            hearing.hearingDescription(),
            hearing.listNote(),
            enrichedWeekCommencing
        );
    }

    private WeekCommencing enrichWeekCommencingWithCourtDetails(final String accessToken, final WeekCommencing weekCommencing) {
        if (isNull(weekCommencing))
            return null;

        final CourtHouseDto courtHouseDto = courtHouseService.getCourtHouseById(
            accessToken,
            weekCommencing.courtHouse().courtHouseId(),
            null
        );

        return new WeekCommencing(
            weekCommencing.startDate(),
            weekCommencing.endDate(),
            weekCommencing.durationInWeeks(),
            courtHouseDto
        );
    }

    private List<CaseDetailsCourtSittingDto> enrichCourtSittingsWithCourtDetails(final String accessToken, final List<CaseDetailsCourtSittingDto> courtSittings
    ) {
        if (isNull(courtSittings))
             return null;

        final String courtHouseId = courtSittings.getFirst().courtHouse().courtHouseId();
        final String courtRoomId = courtSittings.getFirst().courtHouse().courtRoomId();
        final CourtHouseDto courtHouseDto = courtHouseService.getCourtHouseById(accessToken, courtHouseId, courtRoomId);

        return courtSittings.stream()
            .map(cs -> new CaseDetailsCourtSittingDto(
                cs.judiciaryId(),
                cs.sittingStart(),
                cs.sittingEnd(),
                courtHouseDto
            ))
            .toList();
    }
}

