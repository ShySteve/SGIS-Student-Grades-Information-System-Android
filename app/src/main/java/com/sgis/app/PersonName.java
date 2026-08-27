package com.sgis.app;

/**
 * Shared helper for the Surname / First Name / Middle Name (or Initial) / Suffix name fields
 * collected on the Sign Up screen for Students, Teachers, and Admins alike. Storing the parts
 * separately (rather than one free-text "Full Name" field) lets the app alphabetize people by
 * Surname (e.g. the Teacher's student dropdown) without having to guess where a typed name
 * splits.
 */
public class PersonName {
    public final String surname;
    public final String firstName;
    public final String middleName; // optional, blank if not provided
    public final String suffix;     // optional, blank if not provided

    public PersonName(String surname, String firstName, String middleName, String suffix) {
        this.surname = surname == null ? "" : surname.trim();
        this.firstName = firstName == null ? "" : firstName.trim();
        this.middleName = middleName == null ? "" : middleName.trim();
        this.suffix = suffix == null ? "" : suffix.trim();
    }

    /** "Surname, First Name Middle Initial Suffix" - used everywhere a person's name is displayed. */
    public String fullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(surname).append(", ").append(firstName);
        if (!middleName.isEmpty()) {
            sb.append(" ").append(middleInitial());
        }
        if (!suffix.isEmpty()) {
            sb.append(" ").append(suffix);
        }
        return sb.toString();
    }

    private String middleInitial() {
        // Store the full middle name, but display just the initial (standard PH school-form style).
        char c = middleName.charAt(0);
        return Character.toUpperCase(c) + ".";
    }
}
