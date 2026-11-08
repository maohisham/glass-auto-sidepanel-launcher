package com.glass.headunit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** A thin rounded progress bar, optionally with a knob (the song's bar has one, the prayer bar does not). Set the fraction once a second. */
final class BarView extends View {

    private final boolean dark, knob;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), fillP = new Paint(Paint.ANTI_ALIAS_FLAG), knobP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float fraction;
    private final int solid;   // 0 = the accent gradient

    /** Reports a drag: the spot as a fraction, and done=true on release (false while the finger is still down). */
    interface SeekListener { void onSeek(float fraction, boolean done); }

    private SeekListener seekListener;
    private boolean seekable, dragging;

    BarView(Context ctx, boolean dark, boolean knob) { this(ctx, dark, knob, 0); }

    BarView(Context ctx, boolean dark, boolean knob, int solidColor) {
        super(ctx);
        this.solid = solidColor;
        this.dark = dark;
        this.knob = knob;
        track.setColor(dark ? 0x30FFFFFF : 0x2E0C1626);
        knobP.setColor(0xFFFFFFFF);
    }

    /** Turns dragging on or off; a bar that is not seekable only displays. */
    void setSeekable(boolean on, SeekListener l) {
        seekable = on && knob;
        seekListener = l;
        if (!seekable) dragging = false;
    }

    boolean isSeekable() { return seekable; }

    boolean isDragging() { return dragging; }

    private float padPx() { return knob ? Glass.dp(getContext(), 7) : 0; }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        if (!seekable || seekListener == null) return super.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                dragging = true;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);   // a drag here is a seek, not a scroll
                move(e.getX(), false);
                return true;
            case android.view.MotionEvent.ACTION_MOVE:
                if (dragging) move(e.getX(), false);
                return true;
            case android.view.MotionEvent.ACTION_UP:
                if (dragging) { dragging = false; move(e.getX(), true); }
                return true;
            case android.view.MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return true;
        }
    }

    private void move(float x, boolean done) {
        float f = SeekRule.fraction(x, getWidth(), Math.round(padPx()));
        fraction = f;
        invalidate();
        seekListener.onSeek(f, done);
    }

    void setFraction(float f) {
        f = Math.max(0f, Math.min(1f, f));
        if (Math.abs(f - fraction) < 0.0005f) return;
        fraction = f;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(resolveSize(Glass.dp(getContext(), 100), w), Glass.dp(getContext(), knob ? 40 : 10));   // the song bar is tall to touch, drawn thin
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = Glass.dp(getContext(), knob ? 6 : 8), w = getWidth(), top = (getHeight() - h) / 2f, rad = h / 2f;
        float pad = knob ? Glass.dp(getContext(), 7) : 0;
        r.set(pad, top, w - pad, top + h);
        c.drawRoundRect(r, rad, rad, track);
        float end = pad + (w - 2 * pad) * fraction;
        if (end > pad + 1) {
            if (solid != 0) { fillP.setShader(null); fillP.setColor(solid); }
            else fillP.setShader(new LinearGradient(pad, 0, w - pad, 0, PanelStyle.accent(dark), dark ? 0xFF7FF3CF : 0xFF3FD1A1, Shader.TileMode.CLAMP));
            r.set(pad, top, Math.max(end, pad + h), top + h);
            c.drawRoundRect(r, rad, rad, fillP);
        }
        if (knob) c.drawCircle(Math.max(end, pad), getHeight() / 2f, Glass.dp(getContext(), 6), knobP);
    }
}
