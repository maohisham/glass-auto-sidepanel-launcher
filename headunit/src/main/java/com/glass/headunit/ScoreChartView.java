package com.glass.headunit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Score bars over time (days of a week or month, months of a year), each coloured by its score, with a dashed line at
 * 75 (Good). A period without drives shows a small dot. Labels are drawn under the bars; an empty label is skipped.
 */
final class ScoreChartView extends View {

    private final boolean dark;
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG), empty = new Paint(Paint.ANTI_ALIAS_FLAG),
            label = new Paint(Paint.ANTI_ALIAS_FLAG), goal = new Paint(Paint.ANTI_ALIAS_FLAG), today = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int[] values = new int[0];
    private String[] labels = new String[0];
    private int highlight = -1;

    ScoreChartView(Context ctx, boolean dark) {
        super(ctx);
        this.dark = dark;
        empty.setColor(dark ? 0x33FFFFFF : 0x330C1626);
        label.setColor(PanelStyle.dim(dark));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(Glass.dp(ctx, 11));
        goal.setStyle(Paint.Style.STROKE);
        goal.setStrokeWidth(Math.max(1, Glass.dp(ctx, 1)));
        goal.setColor(dark ? 0x5566DD88 : 0x6608906B);
        goal.setPathEffect(new DashPathEffect(new float[]{Glass.dp(ctx, 4), Glass.dp(ctx, 4)}, 0));
        today.setColor(PanelStyle.accent(dark));
    }

    /** values: a score per bar or -1; highlight: the bar for today or this month (-1 for none). */
    void setData(int[] values, String[] labels, int highlight) {
        this.values = values;
        this.labels = labels;
        this.highlight = highlight;
        StringBuilder d = new StringBuilder("Scores: ");
        for (int i = 0; i < values.length; i++) if (values[i] >= 0) d.append(labels[i].isEmpty() ? String.valueOf(i + 1) : labels[i]).append(' ').append(values[i]).append(", ");
        setContentDescription(d.toString());
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(resolveSize(Glass.dp(getContext(), 200), w), Glass.dp(getContext(), 130));
    }

    @Override
    protected void onDraw(Canvas c) {
        int n = values.length;
        if (n == 0) return;
        float w = getWidth(), labelH = Glass.dp(getContext(), 18), top = Glass.dp(getContext(), 6), bottom = getHeight() - labelH;
        float plot = bottom - top, slot = w / n, bw = Math.min(slot * 0.62f, Glass.dp(getContext(), 26)), rad = Math.min(bw / 2f, Glass.dp(getContext(), 5));
        float gy = bottom - plot * 0.75f;
        c.drawLine(0, gy, w, gy, goal);
        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2f;
            int v = values[i];
            if (v < 0) {
                c.drawCircle(cx, bottom - Glass.dp(getContext(), 3), Glass.dp(getContext(), 2.5f), empty);
            } else {
                bar.setColor(PanelStyle.scoreColor(v));
                bar.setAlpha(i == highlight || highlight < 0 ? 255 : 200);
                r.set(cx - bw / 2f, bottom - plot * Math.max(4, v) / 100f, cx + bw / 2f, bottom);
                c.drawRoundRect(r, rad, rad, bar);
            }
            if (i < labels.length && !labels[i].isEmpty()) {
                label.setColor(i == highlight ? PanelStyle.fg(dark) : PanelStyle.dim(dark));
                label.setFakeBoldText(i == highlight);
                c.drawText(labels[i], cx, getHeight() - Glass.dp(getContext(), 4), label);
            }
            if (i == highlight) c.drawCircle(cx, top - Glass.dp(getContext(), 1), Glass.dp(getContext(), 2), today);
        }
    }
}
