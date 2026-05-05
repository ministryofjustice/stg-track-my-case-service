package uk.gov.moj.cp.model.mock;

import lombok.Getter;

@Getter
public enum APIName {

    SLC("CourtSchedule"),
    RCC("ReferenceData"),
    PCD("ProsecutionCase");


    private final String value;

    APIName(String type) {
        this.value = type;
    }
}
