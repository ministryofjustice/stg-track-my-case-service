package uk.gov.moj.cp.model;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;


public enum HearingType {

    TRIAL("Trial"),
    TRIAL_NO_WITNESSES("Trial - no witnesses"),
    TRIAL_OF_ISSUE_NEWTON_HEARING("Trial of Issue / Newton hearing"),
    TRIAL_BACKER("Trial (Backer)"),
    TRIAL_FLOATER("Trial (Floater)"),
    TRIAL_FIRST_WARNING("Trial (First Warning)"),
    TRIAL_PART_HEARD("Trial (Part Heard)"),
    TRIAL_OF_PRELIMINARY_ISSUE("Trial of Preliminary Issue"),
    TRIAL_PRIORITY("Trial (Priority)"),
    TRIAL_PREVIOUSLY_WARNED("Trial (Previously Warned)"),
    TRIAL_RESERVE("Trial (Reserve)"),
    TRIAL_LINKED("Trial Linked"),
    TRIAL_FIXED_THIS_WEEK("Trial (Fixed for this Week)"),
    TRIAL_OF_ISSUE("Trial of issue"),

    SENTENCE("Sentence"),
    SENTENCE_AT_ANOTHER_COURT("Sentence (at another Court)"),
    SENTENCE_OFFICER_TO_ATTEND("Sentence (Officer to Attend)"),
    SENTENCE_PROSECUTION_TO_ATTEND("Sentence (Prosecution to Attend)"),
    SENTENCE_PROSECUTION_AND_OFFICER_TO_ATTEND("Sentence (Prosecution and Officer to Attend)"),
    SENTENCE_PROSECUTION_RELEASED("Sentence (Prosecution Released)");

    private final String value;

    HearingType(String type) {
        this.value = type;
    }

    public String getValue() {
        return value;
    }

    private static final Map<String, HearingType> lookUp =
        Arrays.stream(values())
            .collect(Collectors.toMap(
                v -> v.value.toLowerCase(),
                v -> v
            ));

    public static HearingType fromValue(String value) {
        if (isNull(value)) {
            return null;
        }
        return lookUp.get(value.toLowerCase());
    }
}
