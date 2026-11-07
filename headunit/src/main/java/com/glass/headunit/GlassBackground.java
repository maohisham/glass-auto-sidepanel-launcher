package com.glass.headunit;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** The panel's backdrop: a soft gradient with two gentle colour glows, so the glass buttons have something to show against. Drawn once per size. */
final class GlassBackground extends Drawable {

    private final int[] colors;     // top, bottom, glow A, glow B
    private final Paint base = new Paint(), glowA = new Paint(), glowB = new Paint();

    GlassBackground(boolean dark) { this(dark, null); }

    /** sky is the four colours of a prayer's look (PrayerSky.colors), or null for the usual night blue. */
    GlassBackground(boolean dark, int[] sky) { this.colors = sky != null ? sky : PrayerSky.colors("Isyak", dark); }

    @Override
    protected void onBoundsChange(Rect b) {
        float w = b.width(), h = b.height();
        base.setShader(new LinearGradient(0, 0, 0, h, colors[0], colors[1], Shader.TileMode.CLAMP));
        glowA.setShader(new RadialGradient(w * 0.88f, h * -0.04f, Math.max(w, 1f) * 0.9f, colors[2], colors[2] & 0x00FFFFFF, Shader.TileMode.CLAMP));
        glowB.setShader(new RadialGradient(w * -0.06f, h * 1.04f, Math.max(w, 1f) * 0.8f, colors[3], colors[3] & 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        c.drawRect(b, base);
        c.drawRect(b, glowA);
        c.drawRect(b, glowB);
    }

    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter f) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
