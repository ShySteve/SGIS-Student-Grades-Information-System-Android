# SGIS - Android Port

Native Android (Java + XML layouts) port of the SGIS Swing prototype. Same in-memory data
model and the same functional requirements: login/sign-up for Student/Teacher/Admin, LRN-based
student identification, subject gradebooks with Admin-defined weighted categories, Teacher-entered
scores, and DepEd Order No. 8, s. 2015 grade transmutation.

## Building without Android Studio (GitHub Actions)

A ready-made workflow at `.github/workflows/android-build.yml` builds a debug APK entirely on
GitHub's servers — no Android SDK, Gradle, or Android Studio needed on your machine.

1. Create a new (can be private) repository on [github.com](https://github.com).
2. Push this whole `SGISAndroid` folder to it, e.g.:
   ```
   cd SGISAndroid
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
3. On GitHub, open the **Actions** tab of your repo — the "Build Android APK" workflow will
   already be running (it triggers automatically on push). Wait for it to finish (a few minutes).
4. Click into that completed run, scroll to **Artifacts**, and download `SGIS-debug-apk`. It's a
   zip containing `app-debug.apk` — unzip it, copy the APK to your phone, and install it (you may
   need to allow "install from unknown sources" the first time).

If you don't want to use git from the command line, GitHub's web UI also lets you drag-and-drop
the unzipped folder contents into a new repo via **Add file > Upload files**.

## Features added after the initial port

- **Grades / Attendance navigation** - Student and Teacher dashboards now have two top buttons
  categorizing the two views.
- **Attendance worksheet** - a sheet-style grid (students x class days) per Grade Level + Section,
  built from the real DepEd Three-Term School Calendar for SY 2026-2027 (DepEd Order No. 009,
  s. 2026: June 8, 2026 - April 8, 2027, 201 class days) so only actual class days show up as
  columns. Teachers can tap a cell to cycle Present/Late/Excused/Absent for their advisory
  section; a Student sees a read-only view of just their own row. Movable 2027 holidays (Chinese
  New Year, Holy Week) use DepEd's published estimated dates and may need a small update once
  finalized.
- **"Viewed X ago"** - a Teacher's per-student subject list shows when that student last opened
  their own grade for that subject (or "Not yet viewed"), timestamped the moment the student taps
  into it.
- **Section-wide activities** - grading categories (names/weights) are still Admin-owned and
  schoolwide, but the actual scored items a Teacher adds are now scoped to their Grade Level +
  Section, so adding "Quiz 3" instantly shows up for every other student in that same section/
  subject - without leaking into a different section that happens to share the same subject name.
- **Optional 1x1 profile photo** - Students can upload a photo from their dashboard (auto center-
  cropped to a square); it's stored in-memory as Base64 and shown on their own dashboard. Left
  blank if never uploaded.

## How to open in Android Studio (alternative)

1. Open Android Studio (Koala/2024.1 or newer recommended).
2. **File > Open** and select the `SGISAndroid` folder.
3. Android Studio will offer to regenerate the Gradle wrapper JAR (this project ships the
   wrapper *properties* pointing at Gradle 8.6, but not the binary `gradle-wrapper.jar`, since
   it couldn't be downloaded in the environment this was built in). Accept the prompt, or run
   `gradle wrapper` yourself if you have Gradle installed locally.
4. Let Gradle sync (it will download the Android Gradle Plugin 8.4.0 and AndroidX/Material
   dependencies — needs internet access once).
5. Run on an emulator or device (minSdk 24 / Android 7.0+).

## What changed vs. the Swing version

- **UI toolkit**: `JFrame`/`JPanel`/`JTable`/`JOptionPane` → Android `Activity` + XML layouts +
  `AlertDialog`. Swing can't run on Android at all, so every screen was rebuilt natively; the
  domain/data classes (`Student`, `Teacher`, `Admin`, `GradeCategory`, `GradeItem`,
  `SubjectGradebook`, `DataStore`) are a near-verbatim port — same fields, same methods, same
  grading math (weighted categories → Initial Grade → DepEd transmutation).
- **Shared state**: the `DataStore` that used to get passed directly between `JFrame`
  constructors now lives in a small `Application` subclass (`SgisApp`) and is fetched via
  `SgisApp.store(context)` from each `Activity`. Still purely in-memory — data resets when the
  app process is killed, exactly like the original prototype description.
- **Tables**: `JTable` + `DefaultTableModel` became either a scrollable `LinearLayout` of
  dynamically-inflated rows (subject lists, categories, sections, gradebook items) with
  `EditText`s and `TextWatcher`s standing in for cell-edit listeners, mirroring the original's
  live-update behavior.
- **Dialogs**: `JOptionPane.showInputDialog` became `AlertDialog` + `EditText`.

## Known follow-ups (same as the original's stated next step)

- Data is in-memory only — swapping `DataStore` for Room/SQLite (or a backend) is the natural
  next step, just as the original comment about JDBC/MySQL described.
- No input validation beyond what the original had (e.g. LRN format, numeric ranges).
