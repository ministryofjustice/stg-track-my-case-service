package uk.gov.moj.cp.dto;

public record JudgesResult(String johTitle, String johNameSurname, String role, String johKnownAs) {
    public JudgesResult {
        if (johTitle == null || johNameSurname == null || role == null || johKnownAs == null) {
            throw new IllegalArgumentException("All fields must be non-null");
        }
    }
}
