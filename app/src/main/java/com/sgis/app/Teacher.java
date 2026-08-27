package com.sgis.app;

import java.util.ArrayList;
import java.util.List;

public class Teacher {
    public final String username;
    public final String password;
    public final PersonName name;

    // Advisory (homeroom) section - optional; a Teacher who isn't anyone's adviser leaves both
    // null. Set from a Grade Level provided by an Admin plus one of its Sections. Only the
    // adviser marks/edits their advisory section's daily Attendance.
    public final String advisoryGradeLevel;
    public final String advisorySection;

    // Subject(s) this Teacher is assigned to teach, chosen at sign up from the Admin-created
    // subject list. A Teacher may teach more than one subject.
    public final List<String> subjects = new ArrayList<>();

    // Every Grade Level + Section this Teacher actually has a class in for their subject(s),
    // chosen at sign up from Admin-created Grade Levels/Sections. Stored as "gradeLevel||section"
    // keys (see DataStore#sectionKey). Grade item mirroring (Quiz 1, PT 1, etc.) and the roster
    // shown on the Teacher dashboard are both scoped to this list.
    public final List<String> teachingSections = new ArrayList<>();

    public Teacher(String username, String password, PersonName name,
                    String advisoryGradeLevel, String advisorySection,
                    List<String> subjects, List<String> teachingSections) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.advisoryGradeLevel = advisoryGradeLevel;
        this.advisorySection = advisorySection;
        if (subjects != null) this.subjects.addAll(subjects);
        if (teachingSections != null) this.teachingSections.addAll(teachingSections);
    }

    public boolean hasAdvisorySection() {
        return advisoryGradeLevel != null && advisorySection != null;
    }

    public boolean teachesSubject(String subject) {
        for (String s : subjects) if (s.equalsIgnoreCase(subject)) return true;
        return false;
    }

    public boolean handlesSection(String gradeLevel, String section) {
        return teachingSections.contains(DataStore.sectionKey(gradeLevel, section));
    }
}
