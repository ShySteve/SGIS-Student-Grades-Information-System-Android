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

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private DataStore store;
    private Spinner roleSpinner;
    private TextView idLabel;
    private EditText idField;
    private EditText passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        store = SgisApp.store(this);

        roleSpinner = findViewById(R.id.roleSpinner);
        idLabel = findViewById(R.id.idLabel);
        idField = findViewById(R.id.idField);
        passwordField = findViewById(R.id.passwordField);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Student", "Teacher", "Admin"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        roleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                idLabel.setText(position == 0 ? "LRN:" : "Username:");
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        findViewById(R.id.loginButton).setOnClickListener(v -> handleLogin());
        findViewById(R.id.signUpButton).setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    private void handleLogin() {
        String id = idField.getText().toString().trim();
        String pass = passwordField.getText().toString();
        String role = (String) roleSpinner.getSelectedItem();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please enter both fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        if ("Student".equals(role)) {
            Student s = store.authenticateStudent(id, pass);
            if (s == null) {
                Toast.makeText(this, "Invalid LRN or password.", Toast.LENGTH_SHORT).show();
                return;
            }
            intent = new Intent(this, StudentDashboardActivity.class);
            intent.putExtra("lrn", s.lrn);
        } else if ("Teacher".equals(role)) {
            Teacher t = store.authenticateTeacher(id, pass);
            if (t == null) {
                Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                return;
            }
            intent = new Intent(this, TeacherDashboardActivity.class);
            intent.putExtra("username", t.username);
        } else {
            Admin a = store.authenticateAdmin(id, pass);
            if (a == null) {
                Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                return;
            }
            intent = new Intent(this, AdminDashboardActivity.class);
            intent.putExtra("username", a.username);
        }
        startActivity(intent);
        finish();
    }
}
