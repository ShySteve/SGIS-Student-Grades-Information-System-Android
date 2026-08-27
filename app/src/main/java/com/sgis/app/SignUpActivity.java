package com.sgis.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {

    private DataStore store;
    private Spinner roleSpinner;
    private EditText surnameField;
    private EditText firstNameField;
    private EditText middleNameField;
    private EditText suffixField;
    private TextView idLabel;
    private EditText idField;
    private TextView gradeLevelLabel;
    private Spinner gradeLevelSpinner;
    private TextView sectionLabel;
    private Spinner sectionSpinner;
    private TextView subjectsLabel;
    private View subjectsButton;
    private TextView subjectsSelectedText;
    private TextView teachingSectionsLabel;
    private View teachingSectionsButton;
    private TextView teachingSectionsSelectedText;
    private EditText passwordField;
    private EditText confirmPasswordField;

    private ArrayAdapter<String> gradeLevelAdapter;
    private ArrayAdapter<String> sectionAdapter;

    // Shown as the first Grade Level choice only when signing up as Teacher, since an Advisory
    // section is optional (a Teacher can be subject-only, with no homeroom).
    private static final String NO_ADVISORY_OPTION = "None (not an adviser)";

    // Teacher-only multi-select state.
    private final List<String> selectedSubjects = new ArrayList<>();
    private final List<String> selectedTeachingSectionLabels = new ArrayList<>(); // "Grade Level - Section" display strings

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        store = SgisApp.store(this);

        roleSpinner = findViewById(R.id.roleSpinner);
        surnameField = findViewById(R.id.surnameField);
        firstNameField = findViewById(R.id.firstNameField);
        middleNameField = findViewById(R.id.middleNameField);
        suffixField = findViewById(R.id.suffixField);
        idLabel = findViewById(R.id.idLabel);
        idField = findViewById(R.id.idField);
        gradeLevelLabel = findViewById(R.id.gradeLevelLabel);
        gradeLevelSpinner = findViewById(R.id.gradeLevelSpinner);
        sectionLabel = findViewById(R.id.sectionLabel);
        sectionSpinner = findViewById(R.id.sectionSpinner);
        subjectsLabel = findViewById(R.id.subjectsLabel);
        subjectsButton = findViewById(R.id.subjectsButton);
        subjectsSelectedText = findViewById(R.id.subjectsSelectedText);
        teachingSectionsLabel = findViewById(R.id.teachingSectionsLabel);
        teachingSectionsButton = findViewById(R.id.teachingSectionsButton);
        teachingSectionsSelectedText = findViewById(R.id.teachingSectionsSelectedText);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Student", "Teacher", "Admin"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        // Grade Level choices come from whatever the Admin has already set up.
        gradeLevelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>(store.gradeLevels));
        gradeLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gradeLevelSpinner.setAdapter(gradeLevelAdapter);

        sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sectionSpinner.setAdapter(sectionAdapter);
        refreshSectionSpinner();

        roleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateFieldsForRole();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        gradeLevelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshSectionSpinner();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        updateFieldsForRole();

        subjectsButton.setOnClickListener(v -> promptSelectSubjects());
        teachingSectionsButton.setOnClickListener(v -> promptSelectTeachingSections());

        findViewById(R.id.createAccountButton).setOnClickListener(v -> handleCreateAccount());
        findViewById(R.id.backButton).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    /** Reloads the Grade Level choices, prepending the "not an adviser" option for Teachers only. */
    private void refreshGradeLevelSpinner() {
        String role = (String) roleSpinner.getSelectedItem();
        String previouslySelected = (String) gradeLevelSpinner.getSelectedItem();
        gradeLevelAdapter.clear();
        if ("Teacher".equals(role)) gradeLevelAdapter.add(NO_ADVISORY_OPTION);
        gradeLevelAdapter.addAll(store.gradeLevels);
        gradeLevelAdapter.notifyDataSetChanged();
        if (previouslySelected != null) {
            int idx = gradeLevelAdapter.getPosition(previouslySelected);
            if (idx >= 0) gradeLevelSpinner.setSelection(idx);
        }
    }

    /** Reloads the Section choices to match whichever Grade Level is currently selected. */
    private void refreshSectionSpinner() {
        String grade = (String) gradeLevelSpinner.getSelectedItem();
        sectionAdapter.clear();
        if (grade != null && !NO_ADVISORY_OPTION.equals(grade)) {
            List<String> sections = store.sectionsFor(grade);
            sectionAdapter.addAll(sections);
        }
        sectionAdapter.notifyDataSetChanged();
    }

    private void updateFieldsForRole() {
        String role = (String) roleSpinner.getSelectedItem();
        boolean isStudent = "Student".equals(role);
        boolean isTeacher = "Teacher".equals(role);
        boolean isAdmin = "Admin".equals(role);
        idLabel.setText(isStudent ? "LRN:" : "Username:");
        refreshGradeLevelSpinner();
        refreshSectionSpinner();

        // Grade Level / Section: required for Student (their own class), optional advisory
        // section for Teacher, not applicable to Admin.
        int gradeSectionVisibility = isAdmin ? View.GONE : View.VISIBLE;
        gradeLevelLabel.setText(isTeacher ? "Advisory Grade Level (optional):" : "Grade Level:");
        sectionLabel.setText(isTeacher ? "Advisory Section (optional):" : "Section:");
        gradeLevelLabel.setVisibility(gradeSectionVisibility);
        gradeLevelSpinner.setVisibility(gradeSectionVisibility);
        sectionLabel.setVisibility(gradeSectionVisibility);
        sectionSpinner.setVisibility(gradeSectionVisibility);

        // Subjects / Sections-taught: Teacher only.
        int teacherOnlyVisibility = isTeacher ? View.VISIBLE : View.GONE;
        subjectsLabel.setVisibility(teacherOnlyVisibility);
        subjectsButton.setVisibility(teacherOnlyVisibility);
        subjectsSelectedText.setVisibility(teacherOnlyVisibility);
        teachingSectionsLabel.setVisibility(teacherOnlyVisibility);
        teachingSectionsButton.setVisibility(teacherOnlyVisibility);
        teachingSectionsSelectedText.setVisibility(teacherOnlyVisibility);
    }

    private void promptSelectSubjects() {
        if (store.subjects.isEmpty()) {
            Toast.makeText(this, "No subjects have been set up yet. Please ask an Admin to add one first.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] options = store.subjects.toArray(new String[0]);
        boolean[] checked = new boolean[options.length];
        for (int i = 0; i < options.length; i++) {
            checked[i] = selectedSubjects.contains(options[i]);
        }
        List<String> workingSelection = new ArrayList<>(selectedSubjects);
        new AlertDialog.Builder(this)
                .setTitle("Select the subject(s) you teach")
                .setMultiChoiceItems(options, checked, (dialog, which, isChecked) -> {
                    String option = options[which];
                    if (isChecked) {
                        if (!workingSelection.contains(option)) workingSelection.add(option);
                    } else {
                        workingSelection.remove(option);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedSubjects.clear();
                    selectedSubjects.addAll(workingSelection);
                    subjectsSelectedText.setText(selectedSubjects.isEmpty()
                            ? "None selected yet" : TextUtils.join(", ", selectedSubjects));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptSelectTeachingSections() {
        List<String> allLabels = store.allGradeLevelSectionLabels();
        if (allLabels.isEmpty()) {
            Toast.makeText(this, "No Grade Level / Section options have been set up yet. Please ask an Admin to add one first.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] options = allLabels.toArray(new String[0]);
        boolean[] checked = new boolean[options.length];
        for (int i = 0; i < options.length; i++) {
            checked[i] = selectedTeachingSectionLabels.contains(options[i]);
        }
        List<String> workingSelection = new ArrayList<>(selectedTeachingSectionLabels);
        new AlertDialog.Builder(this)
                .setTitle("Select the section(s) you have classes in")
                .setMultiChoiceItems(options, checked, (dialog, which, isChecked) -> {
                    String option = options[which];
                    if (isChecked) {
                        if (!workingSelection.contains(option)) workingSelection.add(option);
                    } else {
                        workingSelection.remove(option);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedTeachingSectionLabels.clear();
                    selectedTeachingSectionLabels.addAll(workingSelection);
                    teachingSectionsSelectedText.setText(selectedTeachingSectionLabels.isEmpty()
                            ? "None selected yet" : TextUtils.join(", ", selectedTeachingSectionLabels));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleCreateAccount() {
        String role = (String) roleSpinner.getSelectedItem();
        String surname = surnameField.getText().toString().trim();
        String firstName = firstNameField.getText().toString().trim();
        String middleName = middleNameField.getText().toString().trim();
        String suffix = suffixField.getText().toString().trim();
        String id = idField.getText().toString().trim();
        String pass = passwordField.getText().toString();
        String confirmPass = confirmPasswordField.getText().toString();
        boolean isStudent = "Student".equals(role);
        boolean isTeacher = "Teacher".equals(role);
        String gradeLevel = (String) gradeLevelSpinner.getSelectedItem();
        String section = (String) sectionSpinner.getSelectedItem();

        if (TextUtils.isEmpty(surname) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(id) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill in Surname, First Name, and the required fields below.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isStudent && (gradeLevel == null || section == null)) {
            Toast.makeText(this, "No Grade Level / Section options have been set up yet. "
                    + "Please ask an Admin to add one first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (isTeacher && selectedSubjects.isEmpty()) {
            Toast.makeText(this, "Please select at least one subject you teach.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isTeacher && selectedTeachingSectionLabels.isEmpty()) {
            Toast.makeText(this, "Please select at least one section you have classes in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        PersonName name = new PersonName(surname, firstName, middleName, suffix);

        String error;
        if (isStudent) {
            error = store.registerStudent(id, pass, name, gradeLevel, section);
        } else if (isTeacher) {
            // Advisory section is optional for a Teacher - only set it if one was actually picked
            // (as opposed to the "None (not an adviser)" placeholder option).
            boolean hasAdvisory = gradeLevel != null && !NO_ADVISORY_OPTION.equals(gradeLevel) && section != null;
            String advisoryGrade = hasAdvisory ? gradeLevel : null;
            String advisorySection = hasAdvisory ? section : null;
            List<String> teachingSectionKeys = new ArrayList<>();
            for (String label : selectedTeachingSectionLabels) {
                String[] parts = label.split(" - ", 2);
                if (parts.length == 2) teachingSectionKeys.add(DataStore.sectionKey(parts[0], parts[1]));
            }
            error = store.registerTeacher(id, pass, name, advisoryGrade, advisorySection, selectedSubjects, teachingSectionKeys);
        } else {
            error = store.registerAdmin(id, pass, name);
        }

        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Account created! You can now log in.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
