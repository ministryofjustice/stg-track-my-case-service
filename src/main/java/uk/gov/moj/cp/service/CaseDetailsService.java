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


import java.time.Clock;
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

    @Autowired
    private WarningMessageService warningMessageService;

    private Clock clock = Clock.systemDefaultZone();

    public CaseDetailsService() {
    }

    public CaseDetailsService(Clock clock) {
        this.clock = clock;
    }

    public CaseDetailsService(Clock clock, WarningMessageService warningMessageService) {
        this.clock = clock;
        this.warningMessageService = warningMessageService;
    }

    public CaseDetailsDto getCaseDetailsByCaseUrn(String caseUrn) {
        String accessToken = oauthTokenService.getJwtToken();
        List<CourtScheduleDto> courtSchedule = courtScheduleService.getCourtScheduleByCaseUrn(accessToken, caseUrn);

        List<CaseDetailsDto.CaseDetailsCourtScheduleDto> result = courtSchedule.stream()
            .map(schedule -> new CaseDetailsDto.CaseDetailsCourtScheduleDto(
                schedule.hearingDtos().stream()
                    .map(t -> getHearingDetails(accessToken, t))
                    .filter(Objects::nonNull)
                    .sorted(
                        Comparator.comparing(
                                (CaseDetailsHearingDto h) -> h.courtSittings() == null ? null :
                                    h.courtSittings().stream()
                                        .filter(Objects::nonNull)
                                        .map(s -> LocalDateTime.parse(s.sittingStart()))
                                        .min(Comparator.naturalOrder())
                                        .orElse(null),
                                Comparator.nullsLast(Comparator.naturalOrder())
                            )
                            .thenComparingInt((CaseDetailsHearingDto h) ->
                                                  HearingType.TRIAL.getValue().equalsIgnoreCase(h.hearingType()) ? 0 : 1
                            )
                    )
                    .toList()
            ))
            .toList();

        String message = warningMessageService.getMessage(getFirstHearingType(result), getFirstCourtSitting(result));
        CaseDetailsDto caseDetails = new CaseDetailsDto(caseUrn, message, result);

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

    private List<CaseDetailsCourtSittingDto> getFirstCourtSitting(
        List<CaseDetailsDto.CaseDetailsCourtScheduleDto> result) {
        return result.stream()
            .findFirst()
            .map(CaseDetailsDto.CaseDetailsCourtScheduleDto::hearings)
            .filter(hearings -> hearings != null && !hearings.isEmpty())
            .map(hearings -> hearings.getFirst())
            .map(CaseDetailsHearingDto::courtSittings)
            .filter(sittings -> sittings != null && !sittings.isEmpty())
            .orElse(List.of());
    }

private Optional<String> getFirstHearingType(
    List<CaseDetailsDto.CaseDetailsCourtScheduleDto> result) {
    return result.stream()
        .findFirst()
        .map(CaseDetailsDto.CaseDetailsCourtScheduleDto::hearings)
        .filter(hearings -> hearings != null && !hearings.isEmpty())
        .map(hearings -> hearings.getFirst())
        .map(CaseDetailsHearingDto::hearingType);
}

    private CaseDetailsHearingDto getHearingDetails(String accessToken, CourtScheduleDto.HearingDto hearing) {

        if (isNull(hearing) || !isTrailOrSentenceHearing(hearing.hearingType())){
            return null;
        }

        final List<CourtSittingDto> sittings = hearing.courtSittingDtos();
        if (isNull(sittings) || sittings.isEmpty()) {
            return null;
        }

        final boolean hasAnyCurrentOrFutureSitting = sittings.stream()
            .anyMatch(s -> validateSittingDateNotInPast(s.sittingStart()));

        if (!hasAnyCurrentOrFutureSitting) {
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
            hearing.listNote()
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
        LocalDate sittingDate = LocalDateTime.parse(dateTimeString).toLocalDate();
        return !sittingDate.isBefore(LocalDate.now(clock));
    }

    private boolean isTrailOrSentenceHearing(final String hearingType){
        return HearingType.TRIAL.getValue().equalsIgnoreCase(hearingType)
            || HearingType.SENTENCE.getValue().equalsIgnoreCase(hearingType);
    }
}

