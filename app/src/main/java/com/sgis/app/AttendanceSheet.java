package com.sgis.app;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Daily attendance for one Grade Level + Section, keyed by date ("yyyy-MM-dd") then by student
 * LRN, holding a status string. Matches the DepEd School Calendar SY 2026-2027 class days.
 */
public class AttendanceSheet {
    public static final String PRESENT = "Present";
    public static final String LATE = "Late";
    public static final String EXCUSED = "Excused";
    public static final String ABSENT = "Absent";

    public final Map<String, Map<String, String>> byDate = new LinkedHashMap<>();

    public String getStatus(String date, String lrn) {
        Map<String, String> day = byDate.get(date);
        return day != null ? day.get(lrn) : null;
    }

    public void setStatus(String date, String lrn, String status) {
        Map<String, String> day = byDate.get(date);
        if (day == null) {
            day = new LinkedHashMap<>();
            byDate.put(date, day);
        }
        day.put(lrn, status);
    }

    /** Cycles a cell: unset -> Present -> Late -> Excused -> Absent -> Present ... */
    public String cycleStatus(String date, String lrn) {
        String current = getStatus(date, lrn);
        String next;
        if (current == null) next = PRESENT;
        else if (current.equals(PRESENT)) next = LATE;
        else if (current.equals(LATE)) next = EXCUSED;
        else if (current.equals(EXCUSED)) next = ABSENT;
        else next = PRESENT;
        setStatus(date, lrn, next);
        return next;
    }
}
