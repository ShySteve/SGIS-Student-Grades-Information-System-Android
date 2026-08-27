package com.sgis.app;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simplified representation of the DepEd Three-Term School Calendar for SY 2026-2027
 * (DepEd Order No. 009, s. 2026): opens June 8, 2026, closes April 8, 2027, 201 class days.
 * Used to figure out which dates are actual class days for the Attendance worksheet - weekends,
 * the Dec 19, 2026 - Jan 3, 2027 Christmas break, and the published holidays are excluded.
 * Note: movable 2027 holidays (Chinese New Year, Holy Week) use DepEd's published estimated
 * dates as of this writing and may shift slightly once finalized.
 * Dates are represented as plain "yyyy-MM-dd" strings throughout (lexically sortable), rather
 * than java.time, to stay compatible with minSdk 24 without needing core library desugaring.
 */
public class SchoolCalendar {

    public static final String SCHOOL_START = "2026-06-08";
    public static final String SCHOOL_END = "2027-04-08";

    private static final Set<String> NON_CLASS_DATES = new HashSet<>();
    static {
        addRange("2026-12-19", "2027-01-03"); // Christmas break
        addDate("2026-08-21"); // Ninoy Aquino Day (special non-working)
        addDate("2026-08-31"); // National Heroes Day (regular holiday)
        addDate("2026-11-01"); // All Saints' Day
        addDate("2026-11-02"); // All Souls' Day
        addDate("2026-11-30"); // Bonifacio Day (regular holiday)
        addDate("2026-12-08"); // Feast of the Immaculate Conception
        addDate("2027-02-06"); // Chinese New Year (estimated)
        addDate("2027-02-25"); // EDSA Revolution Anniversary
        addDate("2027-03-25"); // Maundy Thursday (estimated, regular holiday)
        addDate("2027-03-26"); // Good Friday (estimated, regular holiday)
        addDate("2027-03-27"); // Black Saturday (estimated)
    }

    private static void addDate(String yyyyMmDd) {
        NON_CLASS_DATES.add(yyyyMmDd);
    }

    private static void addRange(String startYyyyMmDd, String endYyyyMmDd) {
        Calendar cal = parse(startYyyyMmDd);
        Calendar end = parse(endYyyyMmDd);
        while (!cal.after(end)) {
            NON_CLASS_DATES.add(format(cal));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static Calendar parse(String yyyyMmDd) {
        String[] parts = yyyyMmDd.split("-");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        return cal;
    }

    private static String format(Calendar cal) {
        return String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static boolean isWithinSchoolYear(String yyyyMmDd) {
        return yyyyMmDd.compareTo(SCHOOL_START) >= 0 && yyyyMmDd.compareTo(SCHOOL_END) <= 0;
    }

    public static boolean isSchoolDay(String yyyyMmDd) {
        if (!isWithinSchoolYear(yyyyMmDd)) return false;
        Calendar cal = parse(yyyyMmDd);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return false;
        return !NON_CLASS_DATES.contains(yyyyMmDd);
    }

    /** Class days ("yyyy-MM-dd") for the given year/1-based-month, clipped to the school year. */
    public static List<String> classDaysInMonth(int year, int month1based) {
        List<String> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month1based - 1, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = format(cal);
            if (isSchoolDay(dateStr)) result.add(dateStr);
        }
        return result;
    }

    /** [year, month(1-based)] pairs spanning the whole school year, for a month picker. */
    public static List<int[]> schoolMonths() {
        List<int[]> months = new ArrayList<>();
        Calendar cal = parse(SCHOOL_START);
        Calendar end = parse(SCHOOL_END);
        while (true) {
            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH) + 1;
            months.add(new int[]{y, m});
            if (y == end.get(Calendar.YEAR) && m == end.get(Calendar.MONTH) + 1) break;
            cal.add(Calendar.MONTH, 1);
        }
        return months;
    }

    /** The [year, month] pair containing today's date, or the school's first month if today falls outside the school year. */
    public static int[] currentOrFirstMonth() {
        Calendar now = Calendar.getInstance();
        String today = format(now);
        if (isWithinSchoolYear(today)) {
            return new int[]{now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1};
        }
        return schoolMonths().get(0);
    }

    public static String monthLabel(int year, int month1based) {
        String[] names = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return names[month1based - 1] + " " + year;
    }

    /** Short two-line label for a day column header, e.g. "Jun 8\nMon". */
    public static String shortDayLabel(String yyyyMmDd) {
        Calendar cal = parse(yyyyMmDd);
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String[] days = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        return months[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.DAY_OF_MONTH)
                + "\n" + days[cal.get(Calendar.DAY_OF_WEEK)];
    }
}
