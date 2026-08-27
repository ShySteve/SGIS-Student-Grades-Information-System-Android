package com.sgis.app;

import java.util.LinkedHashMap;
import java.util.Map;

public class Student {
    public final String lrn;
    public final String password;
    public final PersonName name;
    public final String gradeLevel;
    public final String section;
    public final Map<String, Double> grades = new LinkedHashMap<>();
    // subject -> last time (System.currentTimeMillis()) the student themself opened that
    // subject's gradebook, used to show Teachers "Viewed 10 mins ago" / "Not yet viewed".
    public final Map<String, Long> lastViewedAt = new LinkedHashMap<>();
    // Optional 1x1 ID-style profile photo, Base64-encoded JPEG. Null/blank if never uploaded.
    // Visible on the Student's own dashboard as well as to any Teacher who handles this
    // student's section for one of their assigned subjects.
    public String photoBase64;

    public Student(String lrn, String password, PersonName name, String gradeLevel, String section) {
        this.lrn = lrn;
        this.password = password;
        this.name = name;
        this.gradeLevel = gradeLevel;
        this.section = section;
    }

    public double computeAverage() {
        if (grades.isEmpty()) return 0.0;
        double sum = 0;
        for (double g : grades.values()) sum += g;
        return sum / grades.size();
    }
}
