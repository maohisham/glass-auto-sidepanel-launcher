package com.glass.headunit;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Supplier;

/**
 * The driving score in detail, opened by tapping the score chip. A row of score rings picks the period (this trip,
 * the last drive, this week, this month, the year so far); below it are the numbers, a chart of the scores over that
 * period, the parts of the score, and a few insights from all the driving kept on the unit.
 */
final class DriveScreen extends LinearLayout {

    private static final int TRIP = 0, LAST = 1, WEEK = 2, MONTH = 3, YEAR = 4;
    private static final String[] NAMES = {"Trip", "Last", "Week", "Month", "YTD"};

    private final boolean dark;
    private final int textSize;
    private final PanelMode mode;
    private final LinearLayout body;
    private final Supplier<List<DriveStats.Drive>> drives;
    private CarCardView card;
    private String cardCar = "";
    private int period = -1;   // -1 until the first refresh picks one

    DriveScreen(Context ctx, boolean dark, int textSize, PanelMode mode, Supplier<List<DriveStats.Drive>> drives, Runnable onClose) {
        super(ctx);
        this.dark = dark;
        this.textSize = textSize;
        this.mode = mode;
        this.drives = drives;
        setOrientation(VERTICAL);
        setBackground(new GlassBackground(dark));
        setClickable(true); // swallow touches so the panel below is not pressed through it
        int pad = Glass.dp(ctx, mode == PanelMode.COMPACT ? 14 : 20);
        setPadding(pad, pad, pad, pad);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new IconView(ctx, Icon.BACK, "Back", dark, 44, IconView.Style.NORMAL, onClose));
        TextView title = text("Driving score", 20, true, PanelStyle.fg(dark));
        LayoutParams tp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tp.leftMargin = Glass.dp(ctx, 12);
        head.addView(title, tp);
        addView(head);

        body = new LinearLayout(ctx);
        body.setOrientation(VERTICAL);
        ScrollView sv = new ScrollView(ctx);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(body);
        addView(sv, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        refresh();
    }

    private TextView text(String s, float baseSp, boolean bold, int color) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, PanelStyle.sp(baseSp, textSize, mode));
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setTextColor(color);
        return t;
    }

    private LayoutParams gap(int topDp) {
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Glass.dp(getContext(), topDp);
        return lp;
    }

    /** Rebuilds the contents from the current trip and the drive log. Call when opened, after a reset, and now and then while open. */
    void refresh() {
        Context ctx = getContext();
        EcoScore trip = DriveTrip.trip();
        long now = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();
        EcoScore.Stats live = trip.isTripActive() ? trip.calculate(now) : null;
        List<DriveStats.Drive> all = drives.get();
        long liveStart = trip.tripStartMs();
        // the running trip counts with its latest numbers, not its last save
        all.removeIf(d -> d.start == liveStart);
        if (live != null && live.distanceKm >= DriveStats.MIN_KM) all.add(DriveStats.Drive.of(live, liveStart, now));
        all.sort((a, b) -> Long.compare(a.start, b.start));
        DriveStats.Drive last = null;
        for (DriveStats.Drive d : all) if (d.start != liveStart) last = d;

        DriveStats.Summary[] sum = new DriveStats.Summary[5];
        sum[LAST] = DriveStats.summarize(last == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(last));
        sum[WEEK] = DriveStats.summarize(DriveStats.between(all, DriveStats.startOfWeek(now, tz), Long.MAX_VALUE));
        sum[MONTH] = DriveStats.summarize(DriveStats.between(all, DriveStats.startOfMonth(now, tz), Long.MAX_VALUE));
        sum[YEAR] = DriveStats.summarize(DriveStats.between(all, DriveStats.startOfYear(now, tz), Long.MAX_VALUE));
        if (period < 0) period = live != null ? TRIP : last != null ? LAST : WEEK;

        body.removeAllViews();
        addCarCard(ctx);
        if (card != null && live != null) card.setEstimate(live.estLPer100);
        body.addView(periodRow(live, sum), gap(12));

        if (period == TRIP) tripDetail(live, trip);
        else periodDetail(period, sum[period], last, all, now, tz);

        List<String> insights = DriveStats.insights(all, now, tz);
        if (!insights.isEmpty()) {
            body.addView(text("Insights", 16, true, PanelStyle.fg(dark)), gap(20));
            for (String s : insights) body.addView(insight(s), gap(8));
        }
    }

    // ---- the period rings ----

    private LinearLayout periodRow(EcoScore.Stats live, DriveStats.Summary[] sum) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        int ring = mode == PanelMode.COMPACT ? 40 : 52;
        for (int p = TRIP; p <= YEAR; p++) {
            final int which = p;
            int score = p == TRIP ? (live == null ? -1 : live.score) : sum[p].score;
            float km = p == TRIP ? (live == null ? 0 : live.distanceKm) : sum[p].km;
            LinearLayout cell = new LinearLayout(getContext());
            cell.setOrientation(VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            int pad = Glass.dp(getContext(), 6);
            cell.setPadding(0, pad, 0, pad);
            cell.setBackground(p == period ? Glass.button(getContext(), dark, false, true, 14) : Glass.pressOverlay(getContext(), dark, 14));
            ScoreRingView rv = new ScoreRingView(getContext(), dark, ring);
            rv.setScore(score);
            cell.addView(rv);
            cell.addView(text(NAMES[p], 12, true, p == period ? PanelStyle.fg(dark) : PanelStyle.dim(dark)), center(4));
            cell.addView(text(score < 0 ? " " : km(km), 11, false, PanelStyle.dim(dark)), center(0));
            cell.setClickable(true);
            cell.setContentDescription(NAMES[p] + (score < 0 ? ", no drives" : ", score " + score));
            cell.setOnClickListener(v -> { period = which; refresh(); });
            LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            lp.leftMargin = lp.rightMargin = Glass.dp(getContext(), 2);
            row.addView(cell, lp);
        }
        return row;
    }

    private LayoutParams center(int topDp) {
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Glass.dp(getContext(), topDp);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        return lp;
    }

    // ---- the running trip ----

    private void tripDetail(EcoScore.Stats s, EcoScore trip) {
        int fg = PanelStyle.fg(dark), dim = PanelStyle.dim(dark);
        if (s == null) {
            body.addView(text("Drive to start scoring", 17, true, fg), gap(16));
            body.addView(text("A trip starts when the car passes 5 km/h and the GPS has a fix. The score updates every few seconds, "
                    + "and every drive is kept for the weekly, monthly and yearly scores.", 14, false, dim), gap(8));
            return;
        }
        body.addView(headline(s.score, s.grade + " driving efficiency"), gap(14));
        body.addView(tiles(new String[][]{
                {km(s.distanceKm), "Distance"},
                {EcoScore.duration(s.durationSec), "Duration"},
                {String.format(Locale.US, "%.0f km/h", s.avgSpeedKmh), "Average"},
                {String.format(Locale.US, "%.0f km/h", s.maxSpeedKmh), "Top speed"},
                {EcoScore.duration(s.idleSec) + String.format(Locale.US, " (%.0f%%)", s.idlePct), "Idle"},
                {s.hasCar ? (s.estLPer100 > 0 ? String.format(Locale.US, "%.1f L/100", s.estLPer100) : "--") : "", s.hasCar ? "Fuel use (est.)" : ""},
        }), gap(12));
        breakdown(s.zoneScore, s.smoothScore, s.effScore, s.idleScore, s.bonusScore, s.hasCar);
        String tip = EcoScore.tip(s);
        if (!tip.isEmpty()) {
            body.addView(text("Tip", 16, true, fg), gap(18));
            body.addView(text(tip, 14, false, dim), gap(4));
        }
        body.addView(Glass.textButton(getContext(), "End this trip", dark, () -> {
            DriveLog.save(getContext(), trip);
            trip.reset();
            period = LAST;
            refresh();
        }), gap(18));
    }

    // ---- a past period ----

    private void periodDetail(int p, DriveStats.Summary s, DriveStats.Drive last, List<DriveStats.Drive> all, long now, TimeZone tz) {
        int dim = PanelStyle.dim(dark);
        if (s.trips == 0) {
            body.addView(text(p == LAST ? "No drives yet" : "No drives " + (p == WEEK ? "this week" : p == MONTH ? "this month" : "this year") + " yet",
                    17, true, PanelStyle.fg(dark)), gap(16));
            body.addView(text("Drives of at least " + Math.round(DriveStats.MIN_KM * 1000) + " m are kept while the panel is open.", 14, false, dim), gap(8));
            return;
        }
        String sub = p == LAST ? when(last.start, last.end, tz) : s.trips + (s.trips == 1 ? " drive" : " drives") + ", best " + s.best;
        body.addView(headline(s.score, EcoScore.grade(s.score) + " · " + sub), gap(14));
        body.addView(tiles(new String[][]{
                {km(s.km), "Distance"},
                {EcoScore.duration(s.sec), "Time driving"},
                {p == LAST ? String.format(Locale.US, "%.0f km/h", s.avgKmh) : String.valueOf(s.trips), p == LAST ? "Average" : "Drives"},
                {s.fuelL > 0 ? String.format(Locale.US, "%.1f L", s.fuelL) : String.format(Locale.US, "%.0f%%", s.idlePct()), s.fuelL > 0 ? "Fuel (est.)" : "Idle"},
        }), gap(12));

        if (p != LAST) {
            ScoreChartView chart = new ScoreChartView(getContext(), dark);
            if (p == WEEK) {
                long from = DriveStats.startOfWeek(now, tz);
                chart.setData(DriveStats.dailyScores(all, from, 7, tz), new String[]{"M", "T", "W", "T", "F", "S", "S"}, daysBetween(from, now, tz));
            } else if (p == MONTH) {
                long from = DriveStats.startOfMonth(now, tz);
                int n = DriveStats.daysInMonth(now, tz);
                String[] labels = new String[n];
                for (int i = 0; i < n; i++) labels[i] = (i == 0 || (i + 1) % 5 == 0) ? String.valueOf(i + 1) : "";
                chart.setData(DriveStats.dailyScores(all, from, n, tz), labels, daysBetween(from, now, tz));
            } else {
                Calendar c = Calendar.getInstance(tz);
                c.setTimeInMillis(now);
                chart.setData(DriveStats.monthlyScores(all, DriveStats.startOfYear(now, tz), tz),
                        new String[]{"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"}, c.get(Calendar.MONTH));
            }
            body.addView(text(p == WEEK ? "Score by day" : p == MONTH ? "Score by day" : "Score by month", 16, true, PanelStyle.fg(dark)), gap(18));
            body.addView(chart, gap(8));
            body.addView(text("Dashed line: Good (75)", 12, false, dim), gap(2));
        }
        breakdown(s.zone, s.smooth, s.eff, s.idle, s.bonus, s.fuelL > 0);
    }

    private static int daysBetween(long from, long now, TimeZone tz) {
        int i = 0;
        long d = from;
        while (i < 31) { long next = DriveStats.addDays(d, 1, tz); if (now < next) return i; d = next; i++; }
        return -1;
    }

    private static String when(long start, long end, TimeZone tz) {
        Calendar a = Calendar.getInstance(tz, Locale.US), b = Calendar.getInstance(tz, Locale.US);
        a.setTimeInMillis(start);
        b.setTimeInMillis(end);
        return String.format(Locale.US, "%ta %<te %<tb, %<tR to %tR", a, b);
    }

    private static String km(float km) { return String.format(Locale.US, km >= 100 ? "%.0f km" : "%.1f km", km); }

    // ---- building blocks ----

    /** The big ring with the score and a line under the grade. */
    private LinearLayout headline(int score, String line) {
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        ScoreRingView ring = new ScoreRingView(getContext(), dark, mode == PanelMode.COMPACT ? 72 : 92);
        ring.setScore(score);
        r.addView(ring);
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(VERTICAL);
        col.addView(text(EcoScore.grade(score), 20, true, PanelStyle.scoreColor(score)));
        col.addView(text(line, 13, false, PanelStyle.dim(dark)));
        LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Glass.dp(getContext(), 14);
        r.addView(col, lp);
        return r;
    }

    /** Small cards of {value, label}, two to a row; an empty pair is skipped. */
    private LinearLayout tiles(String[][] items) {
        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(VERTICAL);
        LinearLayout row = null;
        int inRow = 0;
        for (String[] it : items) {
            if (it[0].isEmpty()) continue;
            if (row == null || inRow == 2) {
                row = new LinearLayout(getContext());
                row.setOrientation(HORIZONTAL);
                grid.addView(row, gap(grid.getChildCount() == 0 ? 0 : 8));
                inRow = 0;
            }
            LinearLayout t = new LinearLayout(getContext());
            t.setOrientation(VERTICAL);
            int pad = Glass.dp(getContext(), 10);
            t.setPadding(pad, pad, pad, pad);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(Glass.dp(getContext(), 12));
            bg.setColor(dark ? 0x14FFFFFF : 0x0D0C1626);
            t.setBackground(bg);
            t.addView(text(it[0], 17, true, PanelStyle.fg(dark)));
            t.addView(text(it[1], 12, false, PanelStyle.dim(dark)));
            LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            if (inRow == 1) lp.leftMargin = Glass.dp(getContext(), 8);
            row.addView(t, lp);
            inRow++;
        }
        if (row != null && inRow == 1) {   // keep a lone last tile half wide
            LayoutParams lp = new LayoutParams(0, 1, 1f);
            lp.leftMargin = Glass.dp(getContext(), 8);
            row.addView(new android.view.View(getContext()), lp);
        }
        return grid;
    }

    private void breakdown(float zone, float smooth, float eff, float idle, float bonus, boolean fuel) {
        body.addView(text("Score breakdown", 16, true, PanelStyle.fg(dark)), gap(18));
        body.addView(bar("Speed zone  35%", zone, 0xFF66DD88), gap(8));
        body.addView(bar("Smoothness  25%", smooth, 0xFF66AAFF), gap(8));
        body.addView(bar(fuel ? "Fuel use  20%" : "Average speed  20%", eff, 0xFFFFCC44), gap(8));
        body.addView(bar("Idling  15%", idle, 0xFFFF9944), gap(8));
        body.addView(bar("Speed cap  5%", bonus, 0xFFCC88FF), gap(8));
    }

    private LinearLayout insight(String s) {
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(HORIZONTAL);
        TextView dot = text("●", 12, true, PanelStyle.accent(dark));
        r.addView(dot);
        LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Glass.dp(getContext(), 8);
        r.addView(text(s, 14, false, PanelStyle.fg(dark)), lp);
        return r;
    }

    /** The car card at the top, when a car is chosen in Settings; the picture is read again only when the car changes. */
    private void addCarCard(Context ctx) {
        Settings st = Settings.of(ctx);
        CarProfile p = CarProfile.byId(st.car());
        if (p == null) { card = null; cardCar = ""; return; }
        if (card == null || !p.id.equals(cardCar)) {
            card = new CarCardView(ctx, dark, p, CarPicture.load(ctx, p.id));
            cardCar = p.id;
        }
        if (card.getParent() != null) ((android.view.ViewGroup) card.getParent()).removeView(card);
        body.addView(card, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout bar(String label, float score, int color) {
        LinearLayout c = new LinearLayout(getContext());
        c.setOrientation(VERTICAL);
        LinearLayout top = new LinearLayout(getContext());
        top.setOrientation(HORIZONTAL);
        top.addView(text(label, 13, false, PanelStyle.dim(dark)), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        top.addView(text(Math.round(score) + "", 13, true, PanelStyle.fg(dark)));
        c.addView(top);
        BarView b = new BarView(getContext(), dark, false, color);
        b.setFraction(score / 100f);
        c.addView(b, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return c;
    }
}
