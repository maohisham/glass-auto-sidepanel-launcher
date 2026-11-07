package com.glass.headunit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;

/** The floating bubble's face: the playing app's icon on a round glass button, or just the music note when nothing plays. */
final class BubbleFace extends View {

    private final boolean dark;
    private final int sizePx;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private Drawable app;

    BubbleFace(Context ctx, boolean dark, int sizePx) {
        super(ctx);
        this.dark = dark;
        this.sizePx = sizePx;
        setBackground(Glass.button(ctx, dark, true, false, 0));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        int c = PanelStyle.fg(dark);
        fill.setColor(c);
        stroke.setColor(c);
    }

    /** The icon to show, or null for the generic music note. */
    void setApp(Drawable d) {
        if (d == app) return;
        app = d;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) { setMeasuredDimension(sizePx, sizePx); }

    @Override
    protected void onDraw(Canvas c) {
        if (app == null) {
            float icon = sizePx * 0.46f;
            c.save();
            c.translate((sizePx - icon) / 2f, (sizePx - icon) / 2f);
            Icons.draw(c, Icon.PLAYER, icon, fill, stroke);
            c.restore();
            return;
        }
        int inset = Math.round(sizePx * 0.14f);
        clip.reset();
        clip.addCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - inset, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        app.setBounds(inset, inset, sizePx - inset, sizePx - inset);
        app.draw(c);
        c.restore();
    }
}
