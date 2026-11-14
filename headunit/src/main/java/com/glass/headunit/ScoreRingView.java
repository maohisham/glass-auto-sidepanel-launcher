package com.glass.headunit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** A ring that fills with the score in its colour (green, amber, red), with the number in the middle; "--" when there is no score. */
final class ScoreRingView extends View {

    private final boolean dark;
    private final int sizeDp;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), arc = new Paint(Paint.ANTI_ALIAS_FLAG), num = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int score = -1;

    ScoreRingView(Context ctx, boolean dark, int sizeDp) {
        super(ctx);
        this.dark = dark;
        this.sizeDp = sizeDp;
        track.setStyle(Paint.Style.STROKE);
        track.setColor(dark ? 0x26FFFFFF : 0x1F0C1626);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        num.setTextAlign(Paint.Align.CENTER);
        num.setTypeface(Typeface.DEFAULT_BOLD);
    }

    void setScore(int s) { score = s; setContentDescription(s < 0 ? "No score" : "Score " + s); invalidate(); }

    @Override
    protected void onMeasure(int w, int h) {
        int px = Glass.dp(getContext(), sizeDp);
        setMeasuredDimension(px, px);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), stroke = Math.max(Glass.dp(getContext(), 4), w * 0.09f), inset = stroke / 2f + 1;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        r.set(inset, inset, w - inset, w - inset);
        c.drawOval(r, track);
        if (score >= 0) {
            arc.setColor(PanelStyle.scoreColor(score));
            c.drawArc(r, -90, 360f * Math.max(1, score) / 100f, false, arc);
        }
        num.setColor(score < 0 ? PanelStyle.dim(dark) : PanelStyle.fg(dark));
        num.setTextSize(w * 0.32f);
        float y = w / 2f - (num.descent() + num.ascent()) / 2f;
        c.drawText(score < 0 ? "--" : String.valueOf(score), w / 2f, y, num);
    }
}
