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
 * The driving score in detail, opened by tapping the score chip. Chips at the top pick the period (this trip, the last
 * drive, this week, this month, the year so far). When there is room (full screen) the page is three columns: the car
 * and the fuel price, the score with its numbers and chart, and why the score is what it is with the insights. In a
 * narrow panel the same parts stack, the score first.
 */
final class DriveScreen extends LinearLayout {

    private static final int TRIP = 0, LAST = 1, WEEK = 2, MONTH = 3, YEAR = 4;
    private static final String[] NAMES = {"Trip", "Last", "Week", "Month", "YTD"};

    private final boolean dark;
    private final int textSize;
    private final PanelMode mode;
    private final LinearLayout page, chips, chipRow, head;
    private LinearLayout out;   // where the section being built goes
    private boolean wide;
    private ScrollView sv;
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
        wide = ScoreLayout.wide(ctx.getResources().getConfiguration().screenWidthDp - 2 * (mode == PanelMode.COMPACT ? 14 : 20));

        head = new LinearLayout(ctx);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new IconView(ctx, Icon.BACK, "Back", dark, 44, IconView.Style.NORMAL, onClose));
        TextView title = text("Driving score", 20, true, PanelStyle.fg(dark));
        LayoutParams tp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tp.leftMargin = Glass.dp(ctx, 12);
        head.addView(title, tp);
        addView(head);

        chips = new LinearLayout(ctx);
        chips.setOrientation(HORIZONTAL);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        chipRow = new LinearLayout(ctx);   // holds the chips under the title when narrow
        addView(chipRow, gap(10));

        page = new LinearLayout(ctx);
        sv = new ScrollView(ctx);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(page);
        LayoutParams sp = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        sp.topMargin = Glass.dp(ctx, 12);
        addView(sv, sp);
        refresh();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        boolean now = ScoreLayout.wide(Math.round((w - getPaddingLeft() - getPaddingRight()) / getResources().getDisplayMetrics().density));
        if (now != wide) { wide = now; post(this::refresh); }
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

        int[] scores = new int[5];
        for (int p = TRIP; p <= YEAR; p++) scores[p] = p == TRIP ? (live == null ? -1 : live.score) : sum[p].score;
        buildChips(scores);

        LinearLayout car = column(), main = column(), why = column(), more = column();
        out = car;
        addCarCard(ctx);
        if (card != null) card.setEstimate(live != null ? live.estLPer100 : lPer100(sum[LAST]));   // with no trip going, the last drive's
        carStatus(ctx);
        if (!wide) out.addView(priceCard(false), gap(out.getChildCount() == 0 ? 0 : 12));   // wide: under the cost line

        out = why;
        why.addView(text(ScoreLayout.why(scores[period]), 16, true, PanelStyle.fg(dark)));
        out = main;
        if (period == TRIP) tripDetail(live, trip, why);
        else periodDetail(period, sum[period], last, all, now, tz, why);
        if (why.getChildCount() == 1) why.removeAllViews();   // no score, nothing to break down

        out = wide ? why : more;
        List<String> insights = DriveStats.insights(all, now, tz);
        if (!insights.isEmpty()) {
            out.addView(text("Insights", 16, true, PanelStyle.fg(dark)), gap(out.getChildCount() == 0 ? 0 : 20));
            for (String s : insights) out.addView(insight(s), gap(8));
        }

        page.removeAllViews();
        if (wide) {
            page.setOrientation(HORIZONTAL);
            addColumn(car, 3, false);
            addColumn(main, 4, true);
            addColumn(why, 3, true);
        } else {
            page.setOrientation(VERTICAL);
            for (LinearLayout c : new LinearLayout[]{main, why, car, more}) {
                if (c.getChildCount() > 0) page.addView(c, gap(page.getChildCount() == 0 ? 0 : 20));
            }
        }
    }

    /** L/100 km a set of drives used (0 when there is no fuel estimate or distance). */
    private static float lPer100(DriveStats.Summary s) { return s != null && s.km > 0 && s.fuelL > 0 ? s.fuelL * 100f / s.km : 0f; }

    private LinearLayout column() {
        LinearLayout c = new LinearLayout(getContext());
        c.setOrientation(VERTICAL);
        return c;
    }

    /** A column of the wide page; the score and the reasons sit on a card, the car column carries its own cards. */
    private void addColumn(LinearLayout c, float weight, boolean onCard) {
        if (onCard && c.getChildCount() > 0) {
            int pad = Glass.dp(getContext(), 16);
            c.setPadding(pad, pad, pad, pad);
            c.setBackground(Glass.button(getContext(), dark, false, false, 16));
        }
        LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, weight);
        if (page.getChildCount() > 0) lp.leftMargin = Glass.dp(getContext(), 14);
        page.addView(c, lp);
    }

    /** The period chips with their scores: in the title row when wide, on their own row under it when narrow. */
    private void buildChips(int[] scores) {
        chips.removeAllViews();
        for (int p = TRIP; p <= YEAR; p++) {
            final int which = p;
            boolean on = p == period;
            TextView c = text(wide ? ScoreLayout.chip(NAMES[p], scores[p]) : ScoreLayout.chipNarrow(NAMES[p], scores[p]), wide ? 14 : 12, true,
                    on ? PanelStyle.fg(dark) : PanelStyle.dim(dark));
            c.setMaxLines(wide ? 1 : 2);
            int h = Glass.dp(getContext(), wide ? 18 : 2), v = Glass.dp(getContext(), 8);   // narrow: every dp goes to "Month"
            c.setPadding(h, v, h, v);
            c.setGravity(Gravity.CENTER);
            c.setBackground(on ? Glass.button(getContext(), dark, false, true, 20) : Glass.pressOverlay(getContext(), dark, 20));
            c.setClickable(true);
            c.setContentDescription(NAMES[p] + (scores[p] < 0 ? ", no drives" : ", score " + scores[p]));
            c.setOnClickListener(x -> { period = which; refresh(); sv.scrollTo(0, 0); });
            LayoutParams lp = wide ? new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT) : new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            if (chips.getChildCount() > 0) lp.leftMargin = Glass.dp(getContext(), wide ? 6 : 3);
            chips.addView(c, lp);
        }
        android.view.ViewGroup want = wide ? head : chipRow;
        if (chips.getParent() != want) {
            if (chips.getParent() != null) ((android.view.ViewGroup) chips.getParent()).removeView(chips);
            LayoutParams lp = wide ? new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT) : new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (wide) lp.leftMargin = Glass.dp(getContext(), 32);
            want.addView(chips, lp);
        }
        chipRow.setVisibility(wide ? GONE : VISIBLE);
    }

    /** This week's RON95 prices in big type: BUDI95 and unsubsidised side by side, readable at a glance. Flat inside
     *  the score card on the wide page (under the cost line), on its own card otherwise. */
    private android.view.View priceCard(boolean flat) {
        Settings st = Settings.of(getContext());
        double[] price = st.fuelPrices();
        int fg = PanelStyle.fg(dark), dim = PanelStyle.dim(dark), accent = PanelStyle.accent(dark);
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        int pad = Glass.dp(getContext(), 16);
        if (!flat) {
            card.setPadding(pad, Glass.dp(getContext(), 12), pad, Glass.dp(getContext(), 14));
            card.setBackground(Glass.button(getContext(), dark, false, false, 16));
        }

        LinearLayout head = new LinearLayout(getContext());
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.addView(text("Fuel price · RON95", 14, true, dim), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        String date = FuelPrice.dateLabel(st.fuelPriceDate());
        head.addView(text(price[1] > 0 ? date : "not fetched yet", 13, false, dim));
        card.addView(head);

        LinearLayout row = new LinearLayout(getContext());
        row.addView(priceCell(price[0], "BUDI95", accent), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        if (price[1] > 0) row.addView(priceCell(price[1], "Unsubsidised", fg), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row, gap(6));

        String saving = FuelPrice.savingLine(price[0], price[1]);
        if (saving != null) card.addView(text(saving, 14, false, accent), gap(8));
        return card;
    }

    private android.view.View priceCell(double rm, String label, int color) {
        LinearLayout c = new LinearLayout(getContext());
        c.setOrientation(VERTICAL);
        LinearLayout value = new LinearLayout(getContext());
        value.setGravity(android.view.Gravity.BOTTOM);
        value.addView(text("RM ", 18, true, color));
        value.addView(text(String.format(Locale.US, "%.2f", rm), 34, true, color));
        c.addView(value);
        c.addView(text(label, 13, false, PanelStyle.dim(dark)));
        return c;
    }

    /** The estimated fuel cost, with and without BUDI95, or how to get one (a car must be chosen for a fuel estimate). */
    private void fuelCost(float litres) {
        double[] price = Settings.of(getContext()).fuelPrices();
        String line = FuelPrice.costLine(litres, price[0], price[1]);
        out.addView(text(line != null ? line : "Choose your car in Settings → Car to see fuel and cost.", 15, line != null,
                line != null ? PanelStyle.fg(dark) : PanelStyle.dim(dark)), gap(10));
        if (wide) out.addView(priceCard(true), gap(16));
    }

    // ---- the running trip ----

    private void tripDetail(EcoScore.Stats s, EcoScore trip, LinearLayout why) {
        int fg = PanelStyle.fg(dark), dim = PanelStyle.dim(dark);
        if (s == null) {
            out.addView(text("Drive to start scoring", 17, true, fg));
            out.addView(text("A trip starts when the car passes 5 km/h and the GPS has a fix. The score updates every few seconds, "
                    + "and every drive is kept for the weekly, monthly and yearly scores.", 14, false, dim), gap(8));
            return;
        }
        out.addView(headline(s.score, "This trip, so far"));
        out.addView(tiles(wide ? 3 : 2, new String[][]{
                {km(s.distanceKm), "Distance"},
                {EcoScore.duration(s.durationSec), "Duration"},
                {String.format(Locale.US, "%.0f km/h", s.avgSpeedKmh), "Average"},
                {String.format(Locale.US, "%.0f km/h", s.maxSpeedKmh), "Top speed"},
                {EcoScore.duration(s.idleSec) + String.format(Locale.US, " (%.0f%%)", s.idlePct), "Idle"},
                {s.hasCar ? (s.estLPer100 > 0 ? String.format(Locale.US, "%.1f L/100", s.estLPer100) : "--") : "", s.hasCar ? "Fuel use (est.)" : ""},
        }), gap(12));
        fuelCost(s.hasCar ? s.estFuelL : 0f);
        breakdown(why, s.zoneScore, s.smoothScore, s.effScore, s.idleScore, s.bonusScore, s.hasCar);
        String tip = EcoScore.tip(s);
        if (!tip.isEmpty()) {
            out.addView(text("Tip", 16, true, fg), gap(18));
            out.addView(text(tip, 14, false, dim), gap(4));
        }
        out.addView(Glass.textButton(getContext(), "End this trip", dark, () -> {
            DriveLog.save(getContext(), trip);
            trip.reset();
            period = LAST;
            refresh();
        }), gap(18));
    }

    // ---- a past period ----

    private void periodDetail(int p, DriveStats.Summary s, DriveStats.Drive last, List<DriveStats.Drive> all, long now, TimeZone tz, LinearLayout why) {
        int dim = PanelStyle.dim(dark);
        if (s.trips == 0) {
            out.addView(text(p == LAST ? "No drives yet" : "No drives " + (p == WEEK ? "this week" : p == MONTH ? "this month" : "this year") + " yet",
                    17, true, PanelStyle.fg(dark)));
            out.addView(text("Drives of at least " + Math.round(DriveStats.MIN_KM * 1000) + " m are kept while the panel is open.", 14, false, dim), gap(8));
            return;
        }
        String sub = p == LAST ? when(last.start, last.end, tz) : s.trips + (s.trips == 1 ? " drive" : " drives") + ", best " + s.best;
        out.addView(headline(s.score, sub));
        out.addView(tiles(wide ? 4 : 2, new String[][]{
                {km(s.km), "Distance"},
                {EcoScore.duration(s.sec), "Time driving"},
                {p == LAST ? String.format(Locale.US, "%.0f km/h", s.avgKmh) : String.valueOf(s.trips), p == LAST ? "Average" : "Drives"},
                {s.fuelL > 0 ? String.format(Locale.US, "%.1f L", s.fuelL) : String.format(Locale.US, "%.0f%%", s.idlePct()), s.fuelL > 0 ? "Fuel (est.)" : "Idle"},
        }), gap(12));
        fuelCost(s.fuelL);

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
            out.addView(text(p == WEEK ? "Score by day" : p == MONTH ? "Score by day" : "Score by month", 16, true, PanelStyle.fg(dark)), gap(18));
            out.addView(chart, gap(8));
            out.addView(text("Dashed line: Good (75)", 12, false, dim), gap(2));
        }
        breakdown(why, s.zone, s.smooth, s.eff, s.idle, s.bonus, s.fuelL > 0);
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
        ScoreRingView ring = new ScoreRingView(getContext(), dark, wide ? 120 : mode == PanelMode.COMPACT ? 72 : 92);
        ring.setScore(score);
        r.addView(ring);
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(VERTICAL);
        col.addView(text(EcoScore.grade(score), wide ? 26 : 20, true, PanelStyle.scoreColor(score)));
        col.addView(text(line, wide ? 15 : 13, false, PanelStyle.dim(dark)));
        LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Glass.dp(getContext(), 14);
        r.addView(col, lp);
        return r;
    }

    /** Small cards of {value, label}, perRow to a row; an empty pair is skipped. */
    private LinearLayout tiles(int perRow, String[][] items) {
        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(VERTICAL);
        LinearLayout row = null;
        int inRow = 0;
        for (String[] it : items) {
            if (it[0].isEmpty()) continue;
            if (row == null || inRow == perRow) {
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
            if (inRow > 0) lp.leftMargin = Glass.dp(getContext(), 8);
            row.addView(t, lp);
            inRow++;
        }
        while (row != null && inRow > 0 && inRow < perRow) {   // keep a short last row's tiles the same width
            LayoutParams lp = new LayoutParams(0, 1, 1f);
            lp.leftMargin = Glass.dp(getContext(), 8);
            row.addView(new android.view.View(getContext()), lp);
            inRow++;
        }
        return grid;
    }

    private void breakdown(LinearLayout why, float zone, float smooth, float eff, float idle, float bonus, boolean fuel) {
        why.addView(bar("Speed zone  35%", zone, 0xFF66DD88), gap(8));
        why.addView(bar("Smoothness  25%", smooth, 0xFF66AAFF), gap(8));
        why.addView(bar(fuel ? "Fuel use  20%" : "Average speed  20%", eff, 0xFFFFCC44), gap(8));
        why.addView(bar("Idling  15%", idle, 0xFFFF9944), gap(8));
        why.addView(bar("Speed cap  5%", bonus, 0xFFCC88FF), gap(8));
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

    /** The odometer, service and road tax entered in Settings, as a short list; due soon is amber, overdue red. */
    private void carStatus(Context ctx) {
        List<CarCards.Row> rows = CarCards.rows(Settings.of(ctx).mileageState(), CarStatus.status(ctx));
        if (rows.isEmpty()) return;
        LinearLayout list = column();
        int pad = Glass.dp(ctx, 16);
        list.setPadding(pad, Glass.dp(ctx, 10), pad, Glass.dp(ctx, 12));
        list.setBackground(Glass.button(ctx, dark, false, false, 16));
        for (CarCards.Row r : rows) {
            int color = r.level == 2 ? 0xFFFF6655 : r.level == 1 ? 0xFFFFCC44 : PanelStyle.fg(dark);
            LinearLayout line = new LinearLayout(ctx);
            line.setOrientation(HORIZONTAL);
            line.addView(text(r.label, 15, false, PanelStyle.dim(dark)), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            line.addView(text(r.value, 16, true, color));
            list.addView(line, gap(list.getChildCount() == 0 ? 0 : 8));
            if (r.sub != null) {
                TextView sub = text(r.sub, 13, false, r.level > 0 ? color : PanelStyle.dim(dark));
                sub.setGravity(Gravity.END);
                list.addView(sub, gap(0));
            }
        }
        out.addView(list, gap(out.getChildCount() == 0 ? 0 : 12));
    }

    /** The car as a short banner, when a car is chosen in Settings; the picture is read again only when the car changes. */
    private void addCarCard(Context ctx) {
        Settings st = Settings.of(ctx);
        CarProfile p = CarProfile.byId(st.car());
        if (p == null) { card = null; cardCar = ""; return; }
        if (card == null || !p.id.equals(cardCar)) {
            card = new CarCardView(ctx, dark, p, CarPicture.load(ctx, p.id));
            cardCar = p.id;
        }
        if (card.getParent() != null) ((android.view.ViewGroup) card.getParent()).removeView(card);
        card.setMaxHeightDp(wide ? 120 : 110);
        out.addView(card, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
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
