package com.glass.headunit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The big controls of the Settings page, sized to be hit and read while driving: switch rows at least 64 dp tall,
 * − / + buttons at least 48 dp, labels at 18 sp and the lines under them at 14 sp, in the panel's glass style.
 */
final class SettingsWidgets {

    private final Context c;
    private final boolean dark;
    private final int fg, dim, accent;

    SettingsWidgets(Context c, boolean dark) {
        this.c = c;
        this.dark = dark;
        fg = PanelStyle.fg(dark);
        dim = PanelStyle.dim(dark);
        accent = PanelStyle.accent(dark);
    }

    int dp(float v) { return Glass.dp(c, v); }

    TextView title(String t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(18f);
        v.setTextColor(fg);
        return v;
    }

    TextView note(String t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(14f);
        v.setTextColor(dim);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    /** A heading for the wide layout: the chosen group's name. */
    TextView heading(String t) {
        TextView v = title(t);
        v.setTextSize(22f);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(4), 0, dp(8));
        return v;
    }

    private LinearLayout row(boolean horizontal) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        l.setPadding(0, dp(10), 0, dp(10));
        if (horizontal) l.setGravity(Gravity.CENTER_VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    private LinearLayout titled(String title, String sub) {
        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(title(title));
        if (sub != null && !sub.isEmpty()) { TextView s = note(sub); s.setTag("sub"); t.addView(s); }
        return t;
    }

    /** Label on the left, a big switch on the right; tapping anywhere on the row flips it. sub may be null. */
    View switchRow(String title, String sub, boolean on, Consumer<Boolean> set) {
        LinearLayout l = row(true);
        l.setMinimumHeight(dp(64));
        l.setBackground(Glass.pressOverlay(c, dark, 10));
        l.addView(titled(title, sub), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        BigSwitch sw = new BigSwitch(c, dark, on);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(12);
        l.addView(sw, lp);
        l.setClickable(true);
        l.setContentDescription(title);
        l.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCheckable(true);
                info.setChecked(sw.isChecked());
            }
        });
        l.setOnClickListener(v -> { sw.setChecked(!sw.isChecked()); set.accept(sw.isChecked()); });
        return l;
    }

    /** The line under a row's title, so a caller can update it (for example the live map share line). */
    TextView subOf(View row) { return row.findViewWithTag("sub"); }

    /** − and + buttons with the value large between them; set gets the new value after each press. */
    View stepper(String title, String sub, int value, String unit, int step, int min, int max, IntConsumer set) {
        LinearLayout l = row(false);
        l.addView(titled(title, sub));
        LinearLayout bar = new LinearLayout(c);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, dp(6), 0, 0);
        final int[] v = {Math.max(min, Math.min(max, value))};
        TextView shown = title(v[0] + unit);
        shown.setTextSize(24f);
        shown.setTypeface(Typeface.DEFAULT_BOLD);
        shown.setGravity(Gravity.CENTER);
        Button less = squareButton("−", title + ": less"), more = squareButton("+", title + ": more");
        less.setOnClickListener(x -> { v[0] = Stepper.next(v[0], -1, step, min, max); shown.setText(v[0] + unit); set.accept(v[0]); });
        more.setOnClickListener(x -> { v[0] = Stepper.next(v[0], 1, step, min, max); shown.setText(v[0] + unit); set.accept(v[0]); });
        bar.addView(less, new LinearLayout.LayoutParams(dp(64), dp(52)));
        bar.addView(shown, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(more, new LinearLayout.LayoutParams(dp(64), dp(52)));
        l.addView(bar);
        return l;
    }

    private Button squareButton(String label, String description) {
        Button b = Glass.textButton(c, label, dark, () -> { });
        b.setTextSize(24f);
        b.setPadding(0, 0, 0, 0);
        b.setContentDescription(description);
        return b;
    }

    private Button choiceButton(String label, boolean on) {
        Button b = Glass.textButton(c, label, dark, () -> { });
        b.setTextSize(16f);
        b.setMinHeight(dp(52));
        mark(b, on);
        return b;
    }

    private void mark(Button b, boolean on) {
        b.setBackground(Glass.button(c, dark, false, on, 14));
        b.setTextColor(on ? accent : fg);
        b.setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        b.setSelected(on);
    }

    /** One row of buttons with the chosen one filled; for two or three choices. */
    View segments(String title, String[] options, int selected, IntConsumer set) {
        return choices(title, options, selected, set, true);
    }

    /** A column of big rows with the chosen one filled; for longer lists. selected may be -1 for none. */
    View list(String title, String[] options, int selected, IntConsumer set) {
        return choices(title, options, selected, set, false);
    }

    private View choices(String title, String[] options, int selected, IntConsumer set, boolean inRow) {
        LinearLayout l = row(false);
        if (title != null) l.addView(title(title));
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(inRow ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        box.setPadding(0, dp(6), 0, 0);
        final Button[] bs = new Button[options.length];
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            Button b = choiceButton(options[i], i == selected);
            if (!inRow) b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            if (!inRow) b.setPadding(dp(16), 0, dp(16), 0);
            b.setOnClickListener(v -> {
                for (int j = 0; j < bs.length; j++) mark(bs[j], j == idx);
                set.accept(idx);
            });
            bs[i] = b;
            LinearLayout.LayoutParams lp = inRow
                    ? new LinearLayout.LayoutParams(0, dp(52), 1f)
                    : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) { if (inRow) lp.leftMargin = dp(6); else lp.topMargin = dp(6); }
            box.addView(b, lp);
        }
        l.addView(box);
        return l;
    }

    /** A full-width glass button, at least 52 dp tall. */
    View bigButton(String label, Runnable r) {
        Button b = Glass.textButton(c, label, dark, r);
        b.setTextSize(16f);
        b.setMinHeight(dp(52));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    /** A section that opens and closes: a header with a chevron, and a body (see bodyOf) shown only while open. */
    LinearLayout fold(String title, boolean open, Consumer<Boolean> toggled) {
        LinearLayout l = row(false);
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setMinimumHeight(dp(56));
        head.setBackground(Glass.pressOverlay(c, dark, 10));
        head.addView(title(title), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView chevron = title(open ? "⌃" : "⌄");
        chevron.setTextSize(22f);
        head.addView(chevron);
        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setTag("body");
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        head.setContentDescription(title);
        head.setOnClickListener(v -> {
            boolean now = body.getVisibility() != View.VISIBLE;
            body.setVisibility(now ? View.VISIBLE : View.GONE);
            chevron.setText(now ? "⌃" : "⌄");
            toggled.accept(now);
        });
        l.addView(head);
        l.addView(body);
        return l;
    }

    LinearLayout bodyOf(LinearLayout fold) { return fold.findViewWithTag("body"); }

    /** One entry of the rail: narrow is an icon over a one-word label, wide is the icon and the group's name in a row. */
    View railItem(Icon icon, String label, boolean wide, boolean selected, boolean dot, Runnable onClick) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        l.setGravity(wide ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL);
        l.setPadding(wide ? dp(16) : 0, dp(wide ? 14 : 9), wide ? dp(12) : 0, dp(wide ? 14 : 9));
        l.setMinimumHeight(dp(wide ? 60 : 64));
        l.setBackground(selected ? Glass.button(c, dark, false, true, 12) : Glass.pressOverlay(c, dark, 12));
        int color = selected ? accent : fg;
        l.addView(new RailIcon(c, icon, color, dot), new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextColor(color);
        t.setTextSize(wide ? 16f : 12f);
        if (selected) t.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (wide) tp.leftMargin = dp(12); else tp.topMargin = dp(2);
        l.addView(t, tp);
        l.setContentDescription(label + (dot ? ", needs attention" : ""));
        l.setSelected(selected);
        l.setOnClickListener(v -> onClick.run());
        return l;
    }

    /** A plain drawn icon, with an optional red dot in its top-right corner. */
    private static final class RailIcon extends View {
        private final Icon icon;
        private final boolean dot;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), red = new Paint(Paint.ANTI_ALIAS_FLAG);

        RailIcon(Context c, Icon icon, int color, boolean dot) {
            super(c);
            this.icon = icon;
            this.dot = dot;
            fill.setColor(color);
            stroke.setColor(color);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2f);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            red.setColor(0xFFFF5544);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas c) {
            float size = Math.min(getWidth(), getHeight());
            Icons.draw(c, icon, size, fill, stroke);
            if (dot) c.drawCircle(size - size * 0.12f, size * 0.12f, size * 0.14f, red);
        }
    }
}
