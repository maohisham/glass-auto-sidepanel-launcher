package com.glass.headunit;

import android.content.Context;
import android.graphics.Insets;
import android.view.WindowInsets;
import android.view.WindowManager;

/** Overlay windows are positioned from below the system bars, not from the screen's corner: this is where their (0, 0) is, in screen px. */
final class OverlayOrigin {

    private OverlayOrigin() { }

    /** {left, top} of the area overlays are measured from. */
    static int[] of(Context ctx) {
        try {
            Insets i = ctx.getSystemService(WindowManager.class).getMaximumWindowMetrics().getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return new int[]{i.left, i.top};
        } catch (Exception e) { return new int[]{0, 0}; }
    }
}
