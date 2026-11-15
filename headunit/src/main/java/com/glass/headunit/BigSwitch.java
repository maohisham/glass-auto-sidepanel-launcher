package com.glass.headunit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** A large drawn on/off switch (58 x 32 dp) for the Settings rows: filled with the accent colour when on. */
final class BigSwitch extends View {

    private final boolean dark;
    private final int w, h;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), knob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private boolean checked;

    BigSwitch(Context c, boolean dark, boolean checked) {
        super(c);
        this.dark = dark;
        this.checked = checked;
        w = Glass.dp(c, 58);
        h = Glass.dp(c, 32);
        knob.setColor(0xFFFFFFFF);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);   // the row carries the switch's meaning
    }

    void setChecked(boolean on) { checked = on; invalidate(); }

    boolean isChecked() { return checked; }

    @Override protected void onMeasure(int a, int b) { setMeasuredDimension(w, h); }

    @Override
    protected void onDraw(Canvas c) {
        track.setColor(checked ? PanelStyle.accent(dark) : (dark ? 0x40FFFFFF : 0x330A1226));
        r.set(0, 0, w, h);
        c.drawRoundRect(r, h / 2f, h / 2f, track);
        float pad = Glass.dp(getContext(), 3), rad = h / 2f - pad;
        float cx = checked ? w - pad - rad : pad + rad;
        c.drawCircle(cx, h / 2f, rad, knob);
    }
}
