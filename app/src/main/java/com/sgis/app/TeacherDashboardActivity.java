package com.sgis.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardActivity extends AppCompatActivity {

    private DataStore store;
    private Teacher teacher;
    private Spinner studentSpinner;
    private ArrayAdapter<String> studentAdapter;
    private LinearLayout subjectsContainer;
    private ImageView studentPhotoImage;

    // display strings ("LRN - Surname, First Name (Grade - Section)") -> lrn, kept in the same
    // alphabetical-by-surname order as the spinner so a position maps straight back to a Student.
    private List<Student> displayedStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);
        store = SgisApp.store(this);
        teacher = store.teachersByUsername.get(getIntent().getStringExtra("username"));
        if (teacher == null) { finish(); return; }
        setTitle("SGIS - " + teacher.name.fullName() + " (Teacher)");

        ((TextView) findViewById(R.id.advisingText)).setText(teacher.hasAdvisorySection()
                ? "Advising: " + teacher.advisoryGradeLevel + " - " + teacher.advisorySection
                : "Advising: none (subject teacher only)");

        studentSpinner = findViewById(R.id.studentSpinner);
        subjectsContainer = findViewById(R.id.subjectsContainer);
        studentPhotoImage = findViewById(R.id.studentPhotoImage);

        studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        studentSpinner.setAdapter(studentAdapter);
        studentSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshStudentPhoto();
                refreshSubjectsTable();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        findViewById(R.id.refreshButton).setOnClickListener(v -> refreshAll());
        findViewById(R.id.navGradesButton).setOnClickListener(v -> refreshSubjectsTable());
        findViewById(R.id.navAttendanceButton).setOnClickListener(v -> {
            if (!teacher.hasAdvisorySection()) {
                Toast.makeText(this, "You're not set as an adviser for any section, so there's no attendance sheet to open.", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, AttendanceActivity.class);
            intent.putExtra("gradeLevel", teacher.advisoryGradeLevel);
            intent.putExtra("section", teacher.advisorySection);
            intent.putExtra("editable", true);
            startActivity(intent);
        });
        findViewById(R.id.signOutButton).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSubjectsTable();
    }

    /** Students across every section this Teacher handles, alphabetized by Surname. */
    private List<Student> rosterForTeacher() {
        return store.studentsInSections(teacher.teachingSections);
    }

    private String displayLabel(Student s) {
        return s.lrn + " - " + s.name.fullName() + " (" + s.gradeLevel + " - " + s.section + ")";
    }

    private Student selectedStudent() {
        int pos = studentSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= displayedStudents.size()) return null;
        return displayedStudents.get(pos);
    }

    /** Reloads the student list (in case new students signed up), preserving the current selection. */
    private void refreshAll() {
        Student previouslySelected = selectedStudent();
        displayedStudents = rosterForTeacher();

        List<String> labels = new ArrayList<>();
        for (Student s : displayedStudents) labels.add(displayLabel(s));

        studentAdapter.clear();
        studentAdapter.addAll(labels);
        studentAdapter.notifyDataSetChanged();

        if (previouslySelected != null) {
            int idx = displayedStudents.indexOf(previouslySelected);
            if (idx >= 0) studentSpinner.setSelection(idx);
        }
        refreshStudentPhoto();
        refreshSubjectsTable();
    }

    /** Shows the selected student's uploaded 1x1 photo (if any), updating whenever the selection changes. */
    private void refreshStudentPhoto() {
        Student s = selectedStudent();
        Bitmap photo = s != null ? ImageUtils.base64ToBitmap(s.photoBase64) : null;
        if (photo != null) {
            studentPhotoImage.setImageBitmap(photo);
        } else {
            studentPhotoImage.setImageDrawable(null); // no photo uploaded for this student
        }
    }

    /** Only the subjects this Teacher is actually assigned to teach - not every subject the student takes. */
    private void refreshSubjectsTable() {
        subjectsContainer.removeAllViews();
        Student s = selectedStudent();
        if (s == null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String subject : teacher.subjects) {
            if (!s.grades.containsKey(subject)) continue; // student's curriculum doesn't include this subject
            View row = inflater.inflate(R.layout.row_subject_grade, subjectsContainer, false);
            ((TextView) row.findViewById(R.id.subjectNameText)).setText(subject);
            ((TextView) row.findViewById(R.id.subjectGradeText)).setText(String.valueOf(s.grades.get(subject)));
            TextView viewedText = row.findViewById(R.id.viewedText);
            viewedText.setText(RelativeTime.format(s.lastViewedAt.get(subject)));
            viewedText.setVisibility(View.VISIBLE);
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, SubjectGradebookActivity.class);
                intent.putExtra("subject", subject);
                intent.putExtra("lrn", s.lrn);
                intent.putExtra("editable", true);
                intent.putExtra("teacherUsername", teacher.username);
                startActivity(intent);
            });
            subjectsContainer.addView(row);
        }
        if (teacher.subjects.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("You don't have any assigned subjects yet.");
            empty.setTextSize(13);
            subjectsContainer.addView(empty);
        }
    }
}
