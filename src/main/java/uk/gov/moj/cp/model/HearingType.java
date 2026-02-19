package uk.gov.moj.cp.model;

public enum HearingType {
    TRIAL("Trial"),
    SENTENCE("Sentence");

    private final String value;

    HearingType(String type) {
        this.value = type;
    }

    public String getValue() {
        return value;
    }
}
