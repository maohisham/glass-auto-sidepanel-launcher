package com.glass.headunit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** The user's own picture for a car: copied into the app's private storage, cropped to the card's shape, one file per car. */
final class CarPicture {

    /** The card picture is stored at this width: sharp enough for any panel, small enough to keep on the unit. */
    static final int STORED_WIDTH = 1200;

    private CarPicture() { }

    static File file(Context ctx, String carId) { return new File(ctx.getFilesDir(), CardCrop.fileName(carId)); }

    static boolean has(Context ctx, String carId) { return file(ctx, carId).length() > 0; }

    /** The stored picture, or null when there is none or it cannot be read. */
    static Bitmap load(Context ctx, String carId) {
        File f = file(ctx, carId);
        if (f.length() == 0) return null;
        return BitmapFactory.decodeFile(f.getAbsolutePath());
    }

    static void remove(Context ctx, String carId) {
        File f = file(ctx, carId);
        if (f.exists() && !f.delete()) Spike.log("car picture: could not remove " + f.getName());
    }

    /** Reads the picture the user chose, crops it to the card's shape, and keeps it for that car. False if it could not be read. */
    static boolean saveFrom(Context ctx, String carId, Uri source) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = ctx.getContentResolver().openInputStream(source)) { BitmapFactory.decodeStream(in, null, bounds); }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = CardCrop.sampleSize(bounds.outWidth, STORED_WIDTH);
            Bitmap decoded;
            try (InputStream in = ctx.getContentResolver().openInputStream(source)) { decoded = BitmapFactory.decodeStream(in, null, opts); }
            if (decoded == null) return false;

            int[] r = CardCrop.rect(decoded.getWidth(), decoded.getHeight(), CardCrop.ASPECT);
            Bitmap cropped = Bitmap.createBitmap(decoded, r[0], r[1], r[2], r[3]);
            Bitmap out = cropped.getWidth() > STORED_WIDTH
                    ? Bitmap.createScaledBitmap(cropped, STORED_WIDTH, Math.round(STORED_WIDTH / CardCrop.ASPECT), true) : cropped;
            File tmp = new File(ctx.getFilesDir(), CardCrop.fileName(carId) + ".tmp");
            try (FileOutputStream fo = new FileOutputStream(tmp)) { out.compress(Bitmap.CompressFormat.JPEG, 85, fo); }
            File dest = file(ctx, carId);
            if (!tmp.renameTo(dest)) { Spike.log("car picture: could not save " + dest.getName()); return false; }
            Spike.log("car picture: saved " + dest.getName() + " (" + out.getWidth() + "x" + out.getHeight() + ")");
            return true;
        } catch (Exception | OutOfMemoryError e) {
            Spike.log("car picture: could not read the picture (" + e.getClass().getSimpleName() + ")");
            return false;
        }
    }
}
