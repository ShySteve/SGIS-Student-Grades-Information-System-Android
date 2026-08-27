package com.sgis.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

/**
 * Detailed per-subject gradebook for one student: categories (PT/WW/Quizzes/etc., set up by an
 * Admin) each holding scored sub-items (e.g. "PT 1", "PT 2") with a score limit, set by a Teacher.
 * Teachers can add sub-items and enter/edit scores; students get a read-only view. Category weights
 * are Admin-owned and are not editable here.
 */
public class SubjectGradebookActivity extends AppCompatActivity {

    private DataStore store;
    private SubjectGradebook gradebook;
    private Student student;
    private String subject;
    private boolean editable;
    private Teacher teacher; // non-null when opened by a Teacher (editable mode); used to mirror items across their other sections

    private LinearLayout categoriesContainer;
    private TextView weightTotalText;
    private TextView initialGradeText;
    private TextView finalGradeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_gradebook);
        store = SgisApp.store(this);

        subject = getIntent().getStringExtra("subject");
        String lrn = getIntent().getStringExtra("lrn");
        editable = getIntent().getBooleanExtra("editable", false);
        String teacherUsername = getIntent().getStringExtra("teacherUsername");
        teacher = teacherUsername != null ? store.teachersByUsername.get(teacherUsername) : null;

        student = store.studentsByLrn.get(lrn);
        if (student == null || subject == null) { finish(); return; }
        gradebook = store.getOrCreateGradebook(student.gradeLevel, student.section, subject);

        if (!editable) {
            student.lastViewedAt.put(subject, System.currentTimeMillis());
        }

        setTitle(subject + " - " + student.name.fullName() + (editable ? " (Teacher view)" : " (Your grades)"));

        categoriesContainer = findViewById(R.id.categoriesContainer);
        weightTotalText = findViewById(R.id.weightTotalText);
        initialGradeText = findViewById(R.id.initialGradeText);
        finalGradeText = findViewById(R.id.finalGradeText);

        ((TextView) findViewById(R.id.headerText)).setText(
                subject + " \u2014 " + student.name.fullName() + " (" + student.lrn + ")");

        findViewById(R.id.closeButton).setOnClickListener(v -> finish());

        rebuildCategoriesUI();
    }

    private void rebuildCategoriesUI() {
        categoriesContainer.removeAllViews();
        double weightTotal = 0;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (GradeCategory cat : gradebook.categories) {
            weightTotal += cat.weightPercent;
            categoriesContainer.addView(buildCategoryView(inflater, cat));
        }

        if (gradebook.categories.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Your admin hasn't set up grading categories (Performance Task, "
                    + "Written Work, Quizzes, etc.) for this subject yet.");
            empty.setTextSize(13);
            categoriesContainer.addView(empty);
        }

        String suffix = Math.abs(weightTotal - 100.0) > 0.01 ? "   (should add up to 100%)" : "   \u2713";
        weightTotalText.setText(String.format("Total category weight (set by Admin): %.1f%%%s", weightTotal, suffix));

        recomputeAndDisplayGrade();
    }

    private View buildCategoryView(LayoutInflater inflater, GradeCategory cat) {
        View view = inflater.inflate(R.layout.view_category, categoriesContainer, false);
        ((TextView) view.findViewById(R.id.categoryTitleText)).setText(
                cat.name + "  (" + trimNumber(cat.weightPercent) + "% of grade \u2014 set by Admin)");

        LinearLayout itemsContainer = view.findViewById(R.id.itemsContainer);
        java.util.Map<String, Double> scores = gradebook.scoresFor(student.lrn);

        for (GradeItem item : cat.items) {
            Double sc = scores.get(item.id);
            View row = inflater.inflate(R.layout.row_item, itemsContainer, false);
            EditText labelField = row.findViewById(R.id.itemLabelField);
            EditText limitField = row.findViewById(R.id.itemLimitField);
            EditText scoreField = row.findViewById(R.id.itemScoreField);

            labelField.setText(item.label);
            limitField.setText(trimNumber(item.limit));
            scoreField.setText(trimNumber(sc != null ? sc : 0.0));

            boolean rowEditable = editable;
            labelField.setEnabled(rowEditable);
            limitField.setEnabled(rowEditable);
            scoreField.setEnabled(rowEditable);

            if (rowEditable) {
                labelField.addTextChangedListener(new SimpleWatcher(s -> {
                    item.label = s.trim();
                    propagateItem(cat, item);
                }));
                limitField.addTextChangedListener(new SimpleWatcher(s -> {
                    try {
                        item.limit = Double.parseDouble(s.trim());
                        propagateItem(cat, item);
                        recomputeAndDisplayGrade();
                    } catch (NumberFormatException ignored) {}
                }));
                scoreField.addTextChangedListener(new SimpleWatcher(s -> {
                    try {
                        double newScore = Double.parseDouble(s.trim());
                        gradebook.scoresFor(student.lrn).put(item.id, newScore);
                        recomputeAndDisplayGrade();
                    } catch (NumberFormatException ignored) {}
                }));
            }

            itemsContainer.addView(row);
        }

        View addItemButton = view.findViewById(R.id.addItemButton);
        if (editable) {
            addItemButton.setOnClickListener(v -> handleAddItem(cat));
        } else {
            addItemButton.setVisibility(View.GONE);
        }

        return view;
    }

    private void handleAddItem(GradeCategory cat) {
        EditText labelInput = new EditText(this);
        labelInput.setHint("e.g. PT 1, Quiz 1");
        new AlertDialog.Builder(this)
                .setTitle("New Item")
                .setMessage("What kind of " + cat.name + " is this?")
                .setView(labelInput)
                .setPositiveButton("Next", (dialog, which) -> {
                    String label = labelInput.getText().toString().trim();
                    if (TextUtils.isEmpty(label)) return;

                    EditText limitInput = new EditText(this);
                    limitInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    limitInput.setHint("e.g. 20");
                    new AlertDialog.Builder(this)
                            .setTitle("Score Limit")
                            .setMessage("Highest possible score for this item:")
                            .setView(limitInput)
                            .setPositiveButton("OK", (d2, w2) -> {
                                try {
                                    double limit = Double.parseDouble(limitInput.getText().toString().trim());
                                    GradeItem newItem = new GradeItem(UUID.randomUUID().toString(), label, limit);
                                    cat.items.add(newItem);
                                    propagateItem(cat, newItem);
                                    rebuildCategoriesUI();
                                } catch (NumberFormatException ex) {
                                    Toast.makeText(this, "Score limit must be a number.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Mirrors an item's label/limit to every other section this Teacher also handles for this
     * subject (e.g. the Teacher teaches the same subject in two sections - adding "Quiz 1" for
     * one instantly shows it, with the same score limit, for the other). No-op for students
     * viewing read-only, or if this gradebook wasn't opened from a Teacher context.
     */
    private void propagateItem(GradeCategory cat, GradeItem item) {
        if (teacher != null) {
            store.propagateItemAcrossTeacherSections(teacher, subject, student.gradeLevel, student.section, cat, item);
        }
    }

    private void recomputeAndDisplayGrade() {
        double initial = gradebook.computeInitialGrade(student.lrn);
        double finalGrade = SubjectGradebook.transmute(initial);

        initialGradeText.setText(String.format("Initial Grade (weighted %%): %.2f", initial));
        finalGradeText.setText(String.format("Final Grade (Transmuted): %.0f", finalGrade));

        // Requirement: computed final grade auto-updates the subject's grade on the dashboards.
        student.grades.put(gradebook.subjectName, finalGrade);
    }

    private static String trimNumber(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    private static class SimpleWatcher implements TextWatcher {
        interface Callback { void onChanged(String text); }
        private final Callback callback;
        SimpleWatcher(Callback callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { callback.onChanged(s.toString()); }
    }
}
