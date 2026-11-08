package com.glass.headunit;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * The cars as drawn pictures, side on, in their real colours (the Myvi silver, the Tiggo black). They are shapes
 * drawn in code, not photographs, so there is nothing to download and they scale to any card. Drawn on a 300 x 176 grid.
 */
final class CarArt {

    static final float W = 300f, H = 176f;

    private CarArt() { }

    private static Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    private static Paint line(int color, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(color);
        return p;
    }

    /** How far the car is lifted on the grid, so its wheels clear the text band along the bottom of the card. */
    private static final float LIFT = -22f;

    /**
     * Draws the car for that id over a studio backdrop that fills the whole width x height box. The whole car is always
     * in view, centred (it is never cropped or squashed); a box wider than the grid just shows more backdrop.
     */
    static void draw(Canvas c, String carId, float width, float height) {
        float scale = Math.min(width / W, height / H);
        float ox = (width - W * scale) / 2f, oy = (height - H * scale) / 2f;
        float floorY = oy + (108f + LIFT) * scale;
        c.drawRect(0, 0, width, height, fill(0xFF23396B));                  // the backdrop
        c.drawRect(0, floorY, width, height, fill(0xFF182A55));             // the floor
        c.drawRect(0, floorY - 2 * scale, width, floorY, fill(0xFF3A5088));
        c.save();
        c.translate(ox, oy + LIFT * scale);
        c.scale(scale, scale);
        if (CarProfile.TIGGO.equals(carId)) tiggo(c); else myvi(c);
        c.restore();
    }

    private static void myvi(Canvas c) {
        c.drawOval(new RectF(46, 109, 254, 123), fill(0xFF0A1226));          // shadow
        Path body = new Path();
        body.moveTo(52, 108); body.lineTo(52, 88); body.quadTo(54, 78, 68, 76); body.lineTo(92, 72);
        body.quadTo(110, 44, 142, 40); body.lineTo(186, 40); body.quadTo(214, 42, 226, 66); body.lineTo(246, 72);
        body.quadTo(258, 76, 258, 90); body.lineTo(258, 108); body.close();
        c.drawPath(body, fill(0xFFC9CED6));
        Path glass = new Path();
        glass.moveTo(100, 70); glass.quadTo(114, 50, 142, 47); glass.lineTo(184, 47); glass.quadTo(206, 49, 216, 68); glass.close();
        c.drawPath(glass, fill(0xFF56677F));
        c.drawLine(150, 47, 150, 70, line(0xFFC9CED6, 3f));
        c.drawLine(62, 90, 246, 90, line(0xFFAAB1BC, 1.5f));
        c.drawRoundRect(new RectF(244, 82, 256, 89), 3, 3, fill(0xFFF4F1D6));   // headlight
        c.drawRoundRect(new RectF(54, 84, 62, 90), 2, 2, fill(0xFFD65A5A));     // tail light
        wheel(c, 94, 108, 17, 9, 0xFF0B0E14, 0xFF9AA1AD);
        wheel(c, 218, 108, 17, 9, 0xFF0B0E14, 0xFF9AA1AD);
        Path shine = new Path();
        shine.moveTo(62, 78); shine.quadTo(90, 74, 116, 73);
        Paint sp = line(0xB3EEF1F5, 2f);
        c.drawPath(shine, sp);
    }

    private static void tiggo(Canvas c) {
        c.drawOval(new RectF(32, 110, 268, 124), fill(0xFF0A1226));
        Path body = new Path();
        body.moveTo(26, 110); body.lineTo(26, 84); body.quadTo(28, 72, 44, 70); body.lineTo(76, 64);
        body.quadTo(90, 40, 128, 36); body.lineTo(204, 36); body.quadTo(236, 38, 252, 62); body.lineTo(272, 70);
        body.quadTo(286, 74, 286, 90); body.lineTo(286, 110); body.close();
        c.drawPath(body, fill(0xFF14171D));
        Path glass = new Path();
        glass.moveTo(90, 62); glass.quadTo(102, 46, 130, 43); glass.lineTo(202, 43); glass.quadTo(226, 45, 240, 62); glass.close();
        c.drawPath(glass, fill(0xFF2C3644));
        c.drawLine(156, 43, 156, 62, line(0xFF14171D, 3f));
        c.drawLine(118, 42, 214, 42, line(0xFF4A5262, 2.5f));                   // roof rail
        c.drawLine(34, 86, 276, 86, line(0xFF2F3541, 1.5f));
        c.drawRoundRect(new RectF(268, 78, 282, 84), 3, 3, fill(0xFFF4F1D6));
        c.drawRoundRect(new RectF(28, 78, 37, 85), 2, 2, fill(0xFFC64A4A));
        wheel(c, 88, 110, 20, 11, 0xFF06080B, 0xFF6F7683);
        wheel(c, 226, 110, 20, 11, 0xFF06080B, 0xFF6F7683);
        Path shine = new Path();
        shine.moveTo(40, 74); shine.quadTo(76, 66, 104, 62);
        c.drawPath(shine, line(0xFF4A5262, 2f));
    }

    private static void wheel(Canvas c, float x, float y, float tyre, float rim, int tyreColor, int rimColor) {
        c.drawCircle(x, y, tyre, fill(tyreColor));
        c.drawCircle(x, y, rim, fill(rimColor));
    }
}
