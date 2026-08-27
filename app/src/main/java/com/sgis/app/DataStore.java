package com.sgis.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataStore {
    public final Map<String, Student> studentsByLrn = new LinkedHashMap<>();
    public final Map<String, Teacher> teachersByUsername = new LinkedHashMap<>();
    public final Map<String, Admin> adminsByUsername = new LinkedHashMap<>();
    // Subjects are created exclusively by Admins (no hardcoded defaults).
    public final List<String> subjects = new ArrayList<>();
    // Admin-owned grading "shape" per subject: category names + weights only (no items - those
    // are per-section, see below). Every section's gradebook for a subject mirrors this shape.
    public final Map<String, List<GradeCategory>> categoryTemplatesBySubject = new LinkedHashMap<>();
    // Actual per-class gradebooks, keyed by "gradeLevel||section||subject". Items (activities/
    // quizzes) and scores live here, scoped to one section, so a Teacher entering an activity for
    // their class immediately reflects to every other student in that same section/subject -
    // without leaking into a different section that happens to take the same subject.
    public final Map<String, SubjectGradebook> gradebooks = new LinkedHashMap<>();
    // Grade Levels are created exclusively by Admins (e.g. "Grade 12"), each with its own list
    // of Sections (e.g. "Pioneers"). These populate the Grade Level / Section choices that
    // Students and Teachers choose from at sign up.
    public final List<String> gradeLevels = new ArrayList<>();
    public final Map<String, List<String>> sectionsByGrade = new LinkedHashMap<>();
    // Daily attendance per section, keyed by "gradeLevel||section".
    public final Map<String, AttendanceSheet> attendanceBySection = new LinkedHashMap<>();

    public static DataStore empty() {
        return new DataStore();
    }

    public Student authenticateStudent(String lrn, String password) {
        Student s = studentsByLrn.get(lrn);
        if (s != null && s.password.equals(password)) return s;
        return null;
    }

    public Teacher authenticateTeacher(String username, String password) {
        Teacher t = teachersByUsername.get(username);
        if (t != null && t.password.equals(password)) return t;
        return null;
    }

    public Admin authenticateAdmin(String username, String password) {
        Admin a = adminsByUsername.get(username);
        if (a != null && a.password.equals(password)) return a;
        return null;
    }

    /**
     * Returns null on success, or an error message if the LRN is already taken.
     * Every subject an Admin has already created is automatically added to the new
     * student's grades (starting at 0) so it shows up premade, per the current curriculum.
     */
    public String registerStudent(String lrn, String password, PersonName name, String gradeLevel, String section) {
        if (studentsByLrn.containsKey(lrn)) return "That LRN is already registered.";
        Student s = new Student(lrn, password, name, gradeLevel, section);
        for (String subject : subjects) {
            s.grades.put(subject, 0.0);
        }
        studentsByLrn.put(lrn, s);
        return null;
    }

    /** Returns null on success, or an error message if the username is already taken. */
    public String registerTeacher(String username, String password, PersonName name,
                                   String advisoryGradeLevel, String advisorySection,
                                   List<String> subjectsTaught, List<String> teachingSections) {
        if (teachersByUsername.containsKey(username)) return "That username is already taken.";
        teachersByUsername.put(username, new Teacher(username, password, name,
                advisoryGradeLevel, advisorySection, subjectsTaught, teachingSections));
        return null;
    }

    /** Returns null on success, or an error message if the username is already taken. */
    public String registerAdmin(String username, String password, PersonName name) {
        if (adminsByUsername.containsKey(username)) return "That username is already taken.";
        adminsByUsername.put(username, new Admin(username, password, name));
        return null;
    }

    /**
     * Adds a subject to the shared subject list if it isn't already there (case-insensitive).
     * Also backfills the subject into every already-registered student's grades map (starting
     * at 0), so it immediately shows up for teachers to grade.
     */
    public void addSubjectIfNew(String subject) {
        for (String existing : subjects) {
            if (existing.equalsIgnoreCase(subject)) return;
        }
        subjects.add(subject);

        for (Student s : studentsByLrn.values()) {
            if (!s.grades.containsKey(subject)) {
                s.grades.put(subject, 0.0);
            }
        }
    }

    /** Gets (or creates) the Admin-owned category shape (names + weights) for a subject. */
    public List<GradeCategory> categoryTemplateFor(String subject) {
        List<GradeCategory> list = categoryTemplatesBySubject.get(subject);
        if (list == null) {
            list = new ArrayList<>();
            categoryTemplatesBySubject.put(subject, list);
        }
        return list;
    }

    /**
     * Ensures a section's gradebook has one category per Admin template category (matched by id),
     * adding any missing ones (with an empty item list) and syncing name/weight on existing ones.
     * Never removes or touches items - those stay owned by the section/Teacher.
     */
    public void syncCategoriesFromTemplate(SubjectGradebook gb, String subject) {
        for (GradeCategory templateCat : categoryTemplateFor(subject)) {
            GradeCategory match = null;
            for (GradeCategory c : gb.categories) {
                if (c.id.equals(templateCat.id)) { match = c; break; }
            }
            if (match == null) {
                gb.categories.add(new GradeCategory(templateCat.id, templateCat.name, templateCat.weightPercent));
            } else {
                match.name = templateCat.name;
                match.weightPercent = templateCat.weightPercent;
            }
        }
    }

    /** Pushes the current Admin template (new categories, renamed/reweighted ones) to every section that already has a gradebook for this subject. */
    public void propagateCategoryTemplate(String subject) {
        for (Map.Entry<String, SubjectGradebook> e : gradebooks.entrySet()) {
            if (e.getKey().endsWith("||" + subject)) {
                syncCategoriesFromTemplate(e.getValue(), subject);
            }
        }
    }

    private static String gradebookKey(String gradeLevel, String section, String subject) {
        return gradeLevel + "||" + section + "||" + subject;
    }

    /** Gets (or creates) the shared gradebook for one section's offering of a subject. */
    public SubjectGradebook getOrCreateGradebook(String gradeLevel, String section, String subject) {
        String key = gradebookKey(gradeLevel, section, subject);
        SubjectGradebook gb = gradebooks.get(key);
        if (gb == null) {
            gb = new SubjectGradebook(subject);
            gradebooks.put(key, gb);
        }
        syncCategoriesFromTemplate(gb, subject);
        return gb;
    }

    /**
     * Mirrors one grade item (its label + score limit, e.g. "Quiz 1" out of 20) across every
     * other Grade Level + Section the given Teacher also handles for this same subject, so
     * entering it once for one of their classes instantly shows it (same label/limit, blank
     * score) for every other class of theirs taking the same subject. Scores are never copied -
     * each section/student's scores stay independent. Matches by the category's id (categories
     * are already kept in sync across sections via the Admin template) and the item's own id, so
     * repeated edits to a label/limit keep re-syncing rather than creating duplicates.
     */
    public void propagateItemAcrossTeacherSections(Teacher teacher, String subject, String sourceGradeLevel,
                                                     String sourceSection, GradeCategory sourceCategory, GradeItem item) {
        if (teacher == null || !teacher.teachesSubject(subject)) return;
        for (String key : teacher.teachingSections) {
            String[] parts = key.split("\\|\\|", 2);
            if (parts.length != 2) continue;
            String gl = parts[0];
            String sec = parts[1];
            if (gl.equals(sourceGradeLevel) && sec.equals(sourceSection)) continue; // skip the source section

            SubjectGradebook targetGb = getOrCreateGradebook(gl, sec, subject);
            GradeCategory targetCat = null;
            for (GradeCategory c : targetGb.categories) {
                if (c.id.equals(sourceCategory.id)) { targetCat = c; break; }
            }
            if (targetCat == null) continue; // shouldn't happen - categories always sync from the Admin template

            GradeItem targetItem = null;
            for (GradeItem gi : targetCat.items) {
                if (gi.id.equals(item.id)) { targetItem = gi; break; }
            }
            if (targetItem == null) {
                targetCat.items.add(new GradeItem(item.id, item.label, item.limit));
            } else {
                targetItem.label = item.label;
                targetItem.limit = item.limit;
            }
        }
    }

    /** Adds a grade level to the shared list if it isn't already there (case-insensitive). */
    public void addGradeLevelIfNew(String gradeLevel) {
        for (String existing : gradeLevels) {
            if (existing.equalsIgnoreCase(gradeLevel)) return;
        }
        gradeLevels.add(gradeLevel);
    }

    /** Gets (or creates) the list of sections that exist under a grade level. */
    public List<String> sectionsFor(String gradeLevel) {
        List<String> list = sectionsByGrade.get(gradeLevel);
        if (list == null) {
            list = new ArrayList<>();
            sectionsByGrade.put(gradeLevel, list);
        }
        return list;
    }

    /**
     * Adds a section under a grade level (creating the grade level first if it doesn't exist yet)
     * if that section isn't already there (case-insensitive).
     */
    public void addSectionIfNew(String gradeLevel, String section) {
        addGradeLevelIfNew(gradeLevel);
        List<String> sections = sectionsFor(gradeLevel);
        for (String existing : sections) {
            if (existing.equalsIgnoreCase(section)) return;
        }
        sections.add(section);
    }

    /** Every "Grade Level - Section" combination that exists across the whole school, e.g. for a Teacher to pick which classes they teach. */
    public List<String> allGradeLevelSectionLabels() {
        List<String> list = new ArrayList<>();
        for (String grade : gradeLevels) {
            for (String section : sectionsFor(grade)) {
                list.add(grade + " - " + section);
            }
        }
        return list;
    }

    /** All students belonging to one Grade Level + Section, alphabetized by Surname (then First Name). */
    public List<Student> studentsInSection(String gradeLevel, String section) {
        List<Student> list = new ArrayList<>();
        for (Student s : studentsByLrn.values()) {
            if (s.gradeLevel.equals(gradeLevel) && s.section.equals(section)) {
                list.add(s);
            }
        }
        sortBySurname(list);
        return list;
    }

    /** All students belonging to any of the given "gradeLevel||section" keys, alphabetized by Surname (then First Name). */
    public List<Student> studentsInSections(List<String> sectionKeys) {
        List<Student> list = new ArrayList<>();
        for (Student s : studentsByLrn.values()) {
            if (sectionKeys.contains(sectionKey(s.gradeLevel, s.section))) {
                list.add(s);
            }
        }
        sortBySurname(list);
        return list;
    }

    private static void sortBySurname(List<Student> list) {
        list.sort(Comparator
                .comparing((Student s) -> s.name.surname, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.name.firstName, String.CASE_INSENSITIVE_ORDER));
    }

    public static String sectionKey(String gradeLevel, String section) {
        return gradeLevel + "||" + section;
    }

    /** Gets (or creates) the daily attendance sheet for one Grade Level + Section. */
    public AttendanceSheet getOrCreateAttendance(String gradeLevel, String section) {
        String key = sectionKey(gradeLevel, section);
        AttendanceSheet sheet = attendanceBySection.get(key);
        if (sheet == null) {
            sheet = new AttendanceSheet();
            attendanceBySection.put(key, sheet);
        }
        return sheet;
    }
}
