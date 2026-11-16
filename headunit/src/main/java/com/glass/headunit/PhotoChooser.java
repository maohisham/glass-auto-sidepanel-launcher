package com.glass.headunit;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Glass Panel's own photo chooser for the car picture: the unit's pictures, newest first, straight from Android's photo
 * library. It needs only the photo permission, so it works where a unit's gallery hands back a link no other app may
 * open (the Eonon). "Other apps…" still opens the unit's own picker.
 */
final class PhotoChooser {

    private PhotoChooser() { }

    /** Shows the chooser; picked gets the chosen picture's link, otherApps opens the unit's own picker. */
    static void show(Activity a, boolean dark, Consumer<Uri> picked, Runnable otherApps) {
        Dialog d = new Dialog(a, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        ExecutorService loader = Executors.newFixedThreadPool(2);
        Handler main = new Handler(Looper.getMainLooper());
        LruCache<Long, Bitmap> thumbs = new LruCache<>(80);
        int fg = PanelStyle.fg(dark), dim = PanelStyle.dim(dark);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GlassBackground(dark));
        int pad = Glass.dp(a, 16);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new IconView(a, Icon.BACK, "Close", dark, 44, IconView.Style.NORMAL, d::dismiss));
        TextView title = new TextView(a);
        title.setText("Choose a picture");
        title.setTextSize(20f);
        title.setTextColor(fg);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = Glass.dp(a, 12);
        head.addView(title, tp);
        head.addView(Glass.textButton(a, "Other apps…", dark, () -> { d.dismiss(); otherApps.run(); }));
        root.addView(head);

        TextView empty = new TextView(a);
        empty.setText("Loading pictures…");
        empty.setTextColor(dim);
        empty.setTextSize(16f);
        empty.setPadding(0, Glass.dp(a, 24), 0, 0);
        root.addView(empty);

        GridView grid = new GridView(a);
        int widthDp = a.getResources().getConfiguration().screenWidthDp;
        grid.setNumColumns(PhotoGrid.columns(widthDp));
        int gap = Glass.dp(a, 8);
        grid.setHorizontalSpacing(gap);
        grid.setVerticalSpacing(gap);
        grid.setVisibility(View.GONE);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        gp.topMargin = Glass.dp(a, 12);
        root.addView(grid, gp);
        d.setContentView(root);

        List<Long> ids = new ArrayList<>();
        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return ids.size(); }
            @Override public Object getItem(int i) { return ids.get(i); }
            @Override public long getItemId(int i) { return ids.get(i); }
            @Override public View getView(int i, View v, ViewGroup parent) {
                ImageView iv = v instanceof ImageView ? (ImageView) v : new ImageView(a);
                int cell = (parent.getWidth() - gap * (PhotoGrid.columns(widthDp) - 1)) / PhotoGrid.columns(widthDp);
                iv.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(cell * 3 / 4, Glass.dp(a, 80))));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setClipToOutline(true);
                iv.setBackground(Glass.button(a, dark, false, false, 10));
                final long id = ids.get(i);
                iv.setTag(id);
                Bitmap b = thumbs.get(id);
                iv.setImageBitmap(b);
                if (b == null) {
                    loader.execute(() -> {
                        Bitmap t;
                        try { t = a.getContentResolver().loadThumbnail(uri(id), new Size(320, 240), null); } catch (Exception e) { t = null; }
                        if (t == null) return;
                        Bitmap done = t;
                        main.post(() -> {
                            thumbs.put(id, done);
                            if (Long.valueOf(id).equals(iv.getTag())) iv.setImageBitmap(done);
                        });
                    });
                }
                return iv;
            }
        };
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, i, id) -> {
            Spike.log("car picture: chosen in the panel's own chooser");
            d.dismiss();
            picked.accept(uri(ids.get(i)));
        });
        d.setOnDismissListener(x -> loader.shutdownNow());
        d.show();

        loader.execute(() -> {
            List<Long> found = query(a);
            main.post(() -> {
                Spike.log("car picture: the chooser found " + found.size() + " pictures");
                ids.addAll(found);
                adapter.notifyDataSetChanged();
                empty.setText(PhotoGrid.EMPTY);
                empty.setVisibility(found.isEmpty() ? View.VISIBLE : View.GONE);
                grid.setVisibility(found.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private static Uri uri(long id) { return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id); }

    /** The newest pictures in the unit's photo library, by id. */
    private static List<Long> query(Activity a) {
        List<Long> out = new ArrayList<>();
        String[] cols = {MediaStore.Images.Media._ID};
        try (Cursor c = a.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC")) {
            if (c == null) return out;
            while (c.moveToNext() && out.size() < PhotoGrid.MAX_PHOTOS) out.add(c.getLong(0));
        } catch (Exception e) {
            Spike.log("car picture: could not list the unit's pictures (" + e.getClass().getSimpleName() + ")");
        }
        return out;
    }
}
