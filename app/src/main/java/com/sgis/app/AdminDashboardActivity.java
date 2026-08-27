package com.sgis.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminDashboardActivity extends AppCompatActivity {

    private DataStore store;
    private Admin admin;

    private Spinner subjectSpinner;
    private ArrayAdapter<String> subjectAdapter;
    private LinearLayout categoriesContainer;
    private TextView weightTotalText;

    private Spinner gradeLevelSpinner;
    private ArrayAdapter<String> gradeLevelAdapter;
    private LinearLayout sectionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        store = SgisApp.store(this);
        admin = store.adminsByUsername.get(getIntent().getStringExtra("username"));
        if (admin == null) { finish(); return; }
        setTitle("SGIS - " + admin.name.fullName() + " (Admin)");

        View subjectsSection = findViewById(R.id.subjectsSection);
        View gradesSection = findViewById(R.id.gradesSection);
        findViewById(R.id.tabSubjectsButton).setOnClickListener(v -> {
            subjectsSection.setVisibility(View.VISIBLE);
            gradesSection.setVisibility(View.GONE);
        });
        findViewById(R.id.tabGradesButton).setOnClickListener(v -> {
            subjectsSection.setVisibility(View.GONE);
            gradesSection.setVisibility(View.VISIBLE);
        });

        setupSubjectsTab();
        setupGradesTab();

        findViewById(R.id.signOutButton).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    /* ---------- Subjects & Grading tab ---------- */

    private void setupSubjectsTab() {
        subjectSpinner = findViewById(R.id.subjectSpinner);
        categoriesContainer = findViewById(R.id.categoriesContainer);
        weightTotalText = findViewById(R.id.weightTotalText);

        subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(store.subjects));
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(subjectAdapter);
        subjectSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshCategories();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        findViewById(R.id.addSubjectButton).setOnClickListener(v -> promptAddSubject());
        findViewById(R.id.addCategoryButton).setOnClickListener(v -> promptAddCategory());

        refreshCategories();
    }

    private List<GradeCategory> selectedCategoryTemplate() {
        String subject = (String) subjectSpinner.getSelectedItem();
        if (subject == null) return null;
        return store.categoryTemplateFor(subject);
    }

    private void promptAddSubject() {
        showTextInputDialog("New Subject", "Subject name (e.g. Piling Larang):", null, name -> {
            if (TextUtils.isEmpty(name)) return;
            name = name.trim();
            store.addSubjectIfNew(name);
            store.categoryTemplateFor(name);

            subjectAdapter.clear();
            subjectAdapter.addAll(store.subjects);
            subjectAdapter.notifyDataSetChanged();
            int idx = subjectAdapter.getPosition(name);
            if (idx >= 0) subjectSpinner.setSelection(idx);

            Toast.makeText(this, "Subject \"" + name + "\" created. It's now been added to every "
                    + "existing student, and will automatically appear for every new student who signs up.",
                    Toast.LENGTH_LONG).show();
            refreshCategories();
        });
    }

    private void promptAddCategory() {
        String subject = (String) subjectSpinner.getSelectedItem();
        if (subject == null) {
            Toast.makeText(this, "Add a subject first.", Toast.LENGTH_SHORT).show();
            return;
        }
        showTextInputDialog("New Category", "Category name (e.g. Performance Task, Written Work, Quizzes, Periodical Test):", null, name -> {
            if (TextUtils.isEmpty(name)) return;
            String trimmedName = name.trim();
            showNumberInputDialog("Category Weight", "What % of the final grade is this category worth (e.g. 60)?", weight -> {
                store.categoryTemplateFor(subject).add(new GradeCategory(UUID.randomUUID().toString(), trimmedName, weight));
                store.propagateCategoryTemplate(subject);
                refreshCategories();
            });
        });
    }

    private void refreshCategories() {
        categoriesContainer.removeAllViews();
        String subject = (String) subjectSpinner.getSelectedItem();
        List<GradeCategory> template = selectedCategoryTemplate();
        if (template == null) {
            weightTotalText.setText("");
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (GradeCategory cat : template) {
            View row = inflater.inflate(R.layout.row_category_editable, categoriesContainer, false);
            EditText nameField = row.findViewById(R.id.categoryNameField);
            EditText weightField = row.findViewById(R.id.categoryWeightField);
            nameField.setText(cat.name);
            weightField.setText(trimNumber(cat.weightPercent));

            nameField.addTextChangedListener(new SimpleWatcher(s -> {
                cat.name = s.trim();
                store.propagateCategoryTemplate(subject);
            }));
            weightField.addTextChangedListener(new SimpleWatcher(s -> {
                try {
                    cat.weightPercent = Double.parseDouble(s.trim());
                    store.propagateCategoryTemplate(subject);
                    updateWeightTotalText(template);
                } catch (NumberFormatException ignored) {
                    // ignore incomplete/invalid input while typing
                }
            }));
            categoriesContainer.addView(row);
        }
        updateWeightTotalText(template);
    }

    private void updateWeightTotalText(List<GradeCategory> template) {
        double total = 0;
        for (GradeCategory cat : template) total += cat.weightPercent;
        String suffix = Math.abs(total - 100.0) > 0.01 ? "  (should add up to 100%)" : "  \u2713";
        weightTotalText.setText(String.format("Total weight: %.1f%%%s", total, suffix));
    }

    /* ---------- Grades & Sections tab ---------- */

    private void setupGradesTab() {
        gradeLevelSpinner = findViewById(R.id.gradeLevelSpinner);
        sectionsContainer = findViewById(R.id.sectionsContainer);

        gradeLevelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(store.gradeLevels));
        gradeLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gradeLevelSpinner.setAdapter(gradeLevelAdapter);
        gradeLevelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshSections();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        findViewById(R.id.addGradeLevelButton).setOnClickListener(v -> promptAddGradeLevel());
        findViewById(R.id.addSectionButton).setOnClickListener(v -> promptAddSection());

        refreshSections();
    }

    private List<String> selectedSections() {
        String grade = (String) gradeLevelSpinner.getSelectedItem();
        if (grade == null) return null;
        return store.sectionsFor(grade);
    }

    private void promptAddGradeLevel() {
        showTextInputDialog("New Grade Level", "New grade level name (e.g. Grade 12):", null, name -> {
            if (TextUtils.isEmpty(name)) return;
            name = name.trim();
            store.addGradeLevelIfNew(name);

            gradeLevelAdapter.clear();
            gradeLevelAdapter.addAll(store.gradeLevels);
            gradeLevelAdapter.notifyDataSetChanged();
            int idx = gradeLevelAdapter.getPosition(name);
            if (idx >= 0) gradeLevelSpinner.setSelection(idx);

            Toast.makeText(this, "Grade level \"" + name + "\" created. Add sections under it below, "
                    + "and it'll show up as a choice on the Sign Up screen.", Toast.LENGTH_LONG).show();
            refreshSections();
        });
    }

    private void promptAddSection() {
        String grade = (String) gradeLevelSpinner.getSelectedItem();
        if (grade == null) {
            Toast.makeText(this, "Add a grade level first.", Toast.LENGTH_SHORT).show();
            return;
        }
        showTextInputDialog("New Section", "New section name under " + grade + " (e.g. Pioneers):", null, name -> {
            if (TextUtils.isEmpty(name)) return;
            store.addSectionIfNew(grade, name.trim());
            refreshSections();
        });
    }

    private void refreshSections() {
        sectionsContainer.removeAllViews();
        List<String> sections = selectedSections();
        if (sections == null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < sections.size(); i++) {
            final int rowIndex = i;
            View row = inflater.inflate(R.layout.row_section_editable, sectionsContainer, false);
            EditText nameField = row.findViewById(R.id.sectionNameField);
            nameField.setText(sections.get(i));
            nameField.addTextChangedListener(new SimpleWatcher(s -> {
                List<String> current = selectedSections();
                if (current != null && rowIndex < current.size() && !TextUtils.isEmpty(s.trim())) {
                    current.set(rowIndex, s.trim());
                }
            }));
            sectionsContainer.addView(row);
        }
    }

    /* ---------- small dialog helpers (stand in for JOptionPane.showInputDialog) ---------- */

    private interface TextResultListener { void onResult(String text); }
    private interface NumberResultListener { void onResult(double value); }

    private void showTextInputDialog(String title, String message, @Nullable String initial, TextResultListener listener) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (initial != null) input.setText(initial);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> listener.onResult(input.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNumberInputDialog(String title, String message, NumberResultListener listener) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    try {
                        double value = Double.parseDouble(input.getText().toString().trim());
                        listener.onResult(value);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(this, "Please enter a valid number.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String trimNumber(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    /** A TextWatcher that just forwards the current text on every change. */
    private static class SimpleWatcher implements TextWatcher {
        interface Callback { void onChanged(String text); }
        private final Callback callback;
        SimpleWatcher(Callback callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { callback.onChanged(s.toString()); }
    }
}
