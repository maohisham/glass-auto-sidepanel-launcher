package com.glass.headunit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * The car card: the car's picture as the background (the user's own photo if they chose one, else the drawn car), and
 * over it, on a darker band, the car's name, its year and colour, and the fuel figures.
 */
final class CarCardView extends FrameLayout {

    private final CarProfile car;
    private final TextView estChip;
    private Bitmap own;

    CarCardView(Context ctx, boolean dark, CarProfile car, Bitmap ownPicture) {
        super(ctx);
        this.car = car;
        this.own = ownPicture;
        float radius = Glass.dp(ctx, 14);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
        });

        addView(new View(ctx) {
            @Override protected void onDraw(Canvas c) {
                if (own != null && !own.isRecycled()) {
                    float s = Math.max(getWidth() / (float) own.getWidth(), getHeight() / (float) own.getHeight());
                    c.save();
                    c.translate((getWidth() - own.getWidth() * s) / 2f, (getHeight() - own.getHeight() * s) / 2f);
                    c.scale(s, s);
                    c.drawBitmap(own, 0, 0, null);
                    c.restore();
                } else {
                    CarArt.draw(c, CarCardView.this.car.id, getWidth(), getHeight());
                }
            }
        }, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout band = new LinearLayout(ctx);
        band.setOrientation(LinearLayout.VERTICAL);
        band.setBackgroundColor(0xC70A1226);
        int pad = Glass.dp(ctx, 12);
        band.setPadding(pad, Glass.dp(ctx, 10), pad, Glass.dp(ctx, 10));
        band.addView(label(ctx, car.name, 16, true, 0xFFFFFFFF));
        band.addView(label(ctx, car.detail, 12, false, 0xB3FFFFFF));
        LinearLayout chips = new LinearLayout(ctx);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.topMargin = Glass.dp(ctx, 6);
        band.addView(chips, cp);
        chips.addView(chip(ctx, String.format(Locale.US, "%.1f L/100 km claimed", car.claimedLPer100), 0xFFFFFFFF));
        estChip = chip(ctx, "Est. --", PanelStyle.accent(true));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-2, -2);
        ep.leftMargin = Glass.dp(ctx, 6);
        chips.addView(estChip, ep);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2);
        bp.gravity = Gravity.BOTTOM;
        addView(band, bp);
    }

    private TextView label(Context ctx, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setTextColor(color);
        t.setSingleLine();
        return t;
    }

    private TextView chip(Context ctx, String s, int color) {
        TextView t = label(ctx, s, 12, false, color);
        int h = Glass.dp(ctx, 8), v = Glass.dp(ctx, 3);
        t.setPadding(h, v, h, v);
        t.setBackground(Glass.button(ctx, true, false, false, 10));
        return t;
    }

    /** Shows the trip's estimated L/100 km (or "--" before there is one). */
    void setEstimate(float lPer100) {
        estChip.setText(lPer100 > 0 ? String.format(Locale.US, "Est. %.1f L/100 km", lPer100) : "Est. --");
    }

    /** Swaps the picture (null goes back to the drawn car). */
    void setOwnPicture(Bitmap b) {
        own = b;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        int height = Math.min(Math.round(width / CardCrop.ASPECT), Glass.dp(getContext(), 210));
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
