package com.glass.headunit;

/** Fits a chosen photo to the car card's shape. Pure. */
final class CardCrop {

    /** The card is five wide to three high. */
    static final float ASPECT = 5f / 3f;

    private CardCrop() { }

    /** {left, top, width, height} of the part of a w x h photo to keep: the middle of it, in the card's shape. */
    static int[] rect(int w, int h, float aspect) {
        float src = w / (float) h;
        if (src > aspect) {
            int cropW = Math.round(h * aspect);
            return new int[]{(w - cropW) / 2, 0, cropW, h};
        }
        int cropH = Math.round(w / aspect);
        return new int[]{0, (h - cropH) / 2, w, cropH};
    }

    /** The largest power of two that still leaves the photo at least targetW wide, so a big photo decodes small. */
    static int sampleSize(int srcW, int targetW) {
        int size = 1;
        while (srcW / (size * 2) >= targetW) size *= 2;
        return size;
    }

    static String fileName(String carId) { return "car_" + carId + ".jpg"; }
}
