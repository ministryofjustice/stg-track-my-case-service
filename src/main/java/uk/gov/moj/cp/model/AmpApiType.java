package uk.gov.moj.cp.model;

import lombok.Getter;

@Getter
public enum AmpApiType {

    SLC("CourtSchedule"),
    RCC("ReferenceData"),
    PCD("ProsecutionCase");


    private final String value;

    AmpApiType(String type) {
        this.value = type;
    }
}
