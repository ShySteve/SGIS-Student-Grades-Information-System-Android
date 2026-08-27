package com.sgis.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A shared gradebook for one subject: the same categories/weights/items apply to the whole class.
 * Only the scores are per-student.
 */
public class SubjectGradebook {
    public final String subjectName;
    public final List<GradeCategory> categories = new ArrayList<>();
    // studentLrn -> (itemId -> score)
    public final Map<String, Map<String, Double>> scoresByStudent = new LinkedHashMap<>();

    public SubjectGradebook(String subjectName) {
        this.subjectName = subjectName;
    }

    public Map<String, Double> scoresFor(String studentLrn) {
        Map<String, Double> m = scoresByStudent.get(studentLrn);
        if (m == null) {
            m = new LinkedHashMap<>();
            scoresByStudent.put(studentLrn, m);
        }
        return m;
    }

    /**
     * Computes the Initial Grade for a student: for each category, Percentage Score =
     * (sum of raw scores / sum of score limits) x 100, then Weighted Score = Percentage Score x
     * (category weight / 100). The Initial Grade is the sum of Weighted Scores across categories.
     * Items with no recorded score yet are skipped (not yet given).
     */
    public double computeInitialGrade(String studentLrn) {
        Map<String, Double> scores = scoresFor(studentLrn);
        double initialGrade = 0;
        for (GradeCategory cat : categories) {
            double totalScore = 0;
            double totalLimit = 0;
            for (GradeItem item : cat.items) {
                Double sc = scores.get(item.id);
                if (sc != null) {
                    totalScore += sc;
                    totalLimit += item.limit;
                }
            }
            double percentageScore = totalLimit > 0 ? (totalScore / totalLimit) * 100.0 : 0.0;
            double weightedScore = percentageScore * (cat.weightPercent / 100.0);
            initialGrade += weightedScore;
        }
        return initialGrade;
    }

    /**
     * Converts an Initial Grade (0-100) into a Final (Transmuted) Grade, matching the piecewise-linear
     * DepEd Order No. 8, s. 2015 transmutation table: Initial Grade 60-100 maps onto Transmuted Grade
     * 75-100, and Initial Grade 0-59.99 maps onto Transmuted Grade 60-74 (minimum report-card grade is 60).
     */
    public static double transmute(double initialGrade) {
        double ig = Math.max(0, Math.min(100, initialGrade));
        double tg;
        if (ig >= 60) {
            tg = 75 + ((ig - 60) / 40.0) * 25.0;
        } else {
            tg = 60 + (ig / 60.0) * 14.0;
        }
        return Math.round(tg);
    }
}
