package uk.gov.moj.cp.model;

public enum HearingType {
    TRIAL("Trial"),
    SENTENCE("Sentence");

    private final String type;

    HearingType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
