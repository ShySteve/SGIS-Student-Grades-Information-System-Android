package com.sgis.app;

import java.util.ArrayList;
import java.util.List;

/** A grading component (e.g. Performance Task, Written Work, Quizzes) with a % weight of the final grade. */
public class GradeCategory {
    public final String id;
    public String name;
    public double weightPercent;
    public final List<GradeItem> items = new ArrayList<>();

    public GradeCategory(String id, String name, double weightPercent) {
        this.id = id;
        this.name = name;
        this.weightPercent = weightPercent;
    }
}
