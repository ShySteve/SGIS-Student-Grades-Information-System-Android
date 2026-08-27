package com.sgis.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final int PHOTO_TARGET_SIZE_PX = 300;

    private DataStore store;
    private Student student;
    private LinearLayout subjectsContainer;
    private TextView averageText;
    private ImageView profileImage;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickedImage);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);
        store = SgisApp.store(this);
        student = store.studentsByLrn.get(getIntent().getStringExtra("lrn"));
        if (student == null) { finish(); return; }
        setTitle("SGIS - " + student.name.fullName());

        ((TextView) findViewById(R.id.nameText)).setText("Name: " + student.name.fullName());
        ((TextView) findViewById(R.id.lrnText)).setText("LRN: " + student.lrn);
        ((TextView) findViewById(R.id.gradeLevelText)).setText("Grade Level: " + student.gradeLevel);
        ((TextView) findViewById(R.id.sectionText)).setText("Section: " + student.section);

        profileImage = findViewById(R.id.profileImage);
        refreshProfilePhoto();

        subjectsContainer = findViewById(R.id.subjectsContainer);
        averageText = findViewById(R.id.averageText);

        findViewById(R.id.uploadPhotoButton).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.navGradesButton).setOnClickListener(v -> refresh());
        findViewById(R.id.navAttendanceButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, AttendanceActivity.class);
            intent.putExtra("gradeLevel", student.gradeLevel);
            intent.putExtra("section", student.section);
            intent.putExtra("editable", false);
            intent.putExtra("focusLrn", student.lrn);
            startActivity(intent);
        });

        findViewById(R.id.signOutButton).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void handlePickedImage(Uri uri) {
        if (uri == null) return; // user backed out of the picker - leave the photo as-is (optional field)
        try {
            Bitmap square = ImageUtils.loadSquareBitmap(getContentResolver(), uri, PHOTO_TARGET_SIZE_PX);
            if (square == null) {
                Toast.makeText(this, "Couldn't read that image.", Toast.LENGTH_SHORT).show();
                return;
            }
            student.photoBase64 = ImageUtils.bitmapToBase64(square);
            profileImage.setImageBitmap(square);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't read that image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshProfilePhoto() {
        Bitmap photo = ImageUtils.base64ToBitmap(student.photoBase64);
        if (photo != null) {
            profileImage.setImageBitmap(photo);
        } else {
            profileImage.setImageDrawable(null); // left blank - photo is optional
        }
    }

    private void refresh() {
        subjectsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> entry : student.grades.entrySet()) {
            String subject = entry.getKey();
            View row = inflater.inflate(R.layout.row_subject_grade, subjectsContainer, false);
            ((TextView) row.findViewById(R.id.subjectNameText)).setText(subject);
            ((TextView) row.findViewById(R.id.subjectGradeText)).setText(
                    String.valueOf(student.grades.get(subject)));
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, SubjectGradebookActivity.class);
                intent.putExtra("subject", subject);
                intent.putExtra("lrn", student.lrn);
                intent.putExtra("editable", false);
                startActivity(intent);
            });
            subjectsContainer.addView(row);
        }
        averageText.setText(String.format("Computed Average: %.2f", student.computeAverage()));
    }
}
