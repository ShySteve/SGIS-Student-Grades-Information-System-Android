package com.sgis.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Attendance worksheet for one Grade Level + Section, "sheet style": students down the rows,
 * class days (per the DepEd SY 2026-2027 calendar) across the columns. Teachers see every
 * registered student in their advisory section and can tap a cell to mark it; a Student opening
 * this from their own dashboard sees only their own row, read-only.
 */
public class AttendanceActivity extends AppCompatActivity {

    private static final int NAME_COL_WIDTH_DP = 130;
    private static final int DAY_COL_WIDTH_DP = 46;
    private static final int ROW_HEIGHT_DP = 40;

    private DataStore store;
    private AttendanceSheet sheet;
    private String gradeLevel;
    private String section;
    private boolean editable;
    private String focusLrn; // non-null when a single Student is viewing just their own row

    private LinearLayout sheetContainer;
    private List<int[]> months;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        store = SgisApp.store(this);

        gradeLevel = getIntent().getStringExtra("gradeLevel");
        section = getIntent().getStringExtra("section");
        editable = getIntent().getBooleanExtra("editable", false);
        focusLrn = getIntent().getStringExtra("focusLrn");

        if (gradeLevel == null || section == null) { finish(); return; }
        sheet = store.getOrCreateAttendance(gradeLevel, section);

        setTitle("Attendance - " + gradeLevel + " " + section);
        ((TextView) findViewById(R.id.headerText)).setText(gradeLevel + " - " + section + " Attendance");
        ((TextView) findViewById(R.id.hintText)).setText(editable
                ? "Tap a cell to cycle Present \u2192 Late \u2192 Excused \u2192 Absent."
                : "Read-only view of your own attendance.");

        sheetContainer = findViewById(R.id.sheetContainer);

        Spinner monthSpinner = findViewById(R.id.monthSpinner);
        months = SchoolCalendar.schoolMonths();
        List<String> monthLabels = new ArrayList<>();
        for (int[] ym : months) monthLabels.add(SchoolCalendar.monthLabel(ym[0], ym[1]));
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthLabels);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        int[] current = SchoolCalendar.currentOrFirstMonth();
        int defaultIndex = 0;
        for (int i = 0; i < months.size(); i++) {
            if (months.get(i)[0] == current[0] && months.get(i)[1] == current[1]) { defaultIndex = i; break; }
        }
        monthSpinner.setSelection(defaultIndex);

        monthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                int[] ym = months.get(position);
                rebuildSheet(ym[0], ym[1]);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        rebuildSheet(current[0], current[1]);

        findViewById(R.id.closeButton).setOnClickListener(v -> finish());
    }

    private void rebuildSheet(int year, int month1based) {
        sheetContainer.removeAllViews();
        List<String> classDays = SchoolCalendar.classDaysInMonth(year, month1based);

        if (classDays.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No class days this month (school break/holiday period).");
            empty.setPadding(0, 16, 0, 16);
            sheetContainer.addView(empty);
            return;
        }

        List<Student> students;
        if (!editable && focusLrn != null) {
            Student self = store.studentsByLrn.get(focusLrn);
            students = new ArrayList<>();
            if (self != null) students.add(self);
        } else {
            students = store.studentsInSection(gradeLevel, section);
        }

        if (students.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No registered students found for this section yet.");
            empty.setPadding(0, 16, 0, 16);
            sheetContainer.addView(empty);
            return;
        }

        // Header row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.addView(makeNameCell("Student", true, Color.parseColor("#EDEDED")));
        for (String date : classDays) {
            headerRow.addView(makeDayHeaderCell(SchoolCalendar.shortDayLabel(date)));
        }
        sheetContainer.addView(headerRow);

        // One row per student
        for (Student s : students) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(makeNameCell(s.name.fullName(), false, Color.WHITE));
            for (String date : classDays) {
                row.addView(makeStatusCell(date, s.lrn));
            }
            sheetContainer.addView(row);
        }
    }

    private TextView makeNameCell(String text, boolean bold, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setWidth(dp(NAME_COL_WIDTH_DP));
        tv.setHeight(dp(ROW_HEIGHT_DP));
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        tv.setPadding(dp(6), 0, dp(4), 0);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        tv.setBackgroundColor(bgColor);
        return tv;
    }

    private TextView makeDayHeaderCell(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setWidth(dp(DAY_COL_WIDTH_DP));
        tv.setHeight(dp(ROW_HEIGHT_DP));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setBackgroundColor(Color.parseColor("#EDEDED"));
        return tv;
    }

    private TextView makeStatusCell(String date, String lrn) {
        TextView cell = new TextView(this);
        cell.setWidth(dp(DAY_COL_WIDTH_DP));
        cell.setHeight(dp(ROW_HEIGHT_DP));
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cell.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(1), dp(1), 0, 0);
        cell.setLayoutParams(params);

        applyStatusStyle(cell, sheet.getStatus(date, lrn));

        if (editable) {
            cell.setOnClickListener(v -> {
                String newStatus = sheet.cycleStatus(date, lrn);
                applyStatusStyle(cell, newStatus);
            });
        }
        return cell;
    }

    private void applyStatusStyle(TextView cell, String status) {
        if (status == null) {
            cell.setText("");
            cell.setBackgroundColor(Color.parseColor("#F5F5F5"));
            return;
        }
        switch (status) {
            case AttendanceSheet.PRESENT:
                cell.setText("P");
                cell.setBackgroundColor(Color.parseColor("#C8E6C9"));
                cell.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case AttendanceSheet.LATE:
                cell.setText("L");
                cell.setBackgroundColor(Color.parseColor("#FFF9C4"));
                cell.setTextColor(Color.parseColor("#F57F17"));
                break;
            case AttendanceSheet.EXCUSED:
                cell.setText("E");
                cell.setBackgroundColor(Color.parseColor("#BBDEFB"));
                cell.setTextColor(Color.parseColor("#1565C0"));
                break;
            case AttendanceSheet.ABSENT:
                cell.setText("A");
                cell.setBackgroundColor(Color.parseColor("#FFCDD2"));
                cell.setTextColor(Color.parseColor("#C62828"));
                break;
            default:
                cell.setText("");
                cell.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
