package com.sgis.app;

/** A single scored item within a category, e.g. "Quiz 1" with a highest possible score of 20. */
public class GradeItem {
    public final String id;
    public String label;
    public double limit; // highest possible score for this item

    public GradeItem(String id, String label, double limit) {
        this.id = id;
        this.label = label;
        this.limit = limit;
    }
}
