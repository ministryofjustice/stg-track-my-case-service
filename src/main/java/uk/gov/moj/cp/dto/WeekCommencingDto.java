package uk.gov.moj.cp.dto;

public record WeekCommencingDto(
    String courtHouse,
    String startDate,
    String endDate,
    int durationInWeeks
) {
}
