package com.glass.headunit;

import android.content.Context;
import android.graphics.Insets;
import android.view.WindowInsets;
import android.view.WindowManager;

/** Overlay windows are positioned from below the system bars, not from the screen's corner: this is where their (0, 0) is, in screen px. */
final class OverlayOrigin {

    private OverlayOrigin() { }

    /** {width, height} of the area overlays can use: the screen without the system bars. */
    static int[] area(Context ctx) {
        try {
            WindowManager wm = ctx.getSystemService(WindowManager.class);
            android.graphics.Rect b = wm.getMaximumWindowMetrics().getBounds();
            Insets i = wm.getMaximumWindowMetrics().getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return new int[]{b.width() - i.left - i.right, b.height() - i.top - i.bottom};
        } catch (Exception e) {
            android.graphics.Rect b = ctx.getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();
            return new int[]{b.width(), b.height()};
        }
    }

    /** {left, top} of the area overlays are measured from. */
    static int[] of(Context ctx) {
        try {
            Insets i = ctx.getSystemService(WindowManager.class).getMaximumWindowMetrics().getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return new int[]{i.left, i.top};
        } catch (Exception e) { return new int[]{0, 0}; }
    }
}
