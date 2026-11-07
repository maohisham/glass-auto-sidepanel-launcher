package com.glass.headunit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The apps grid: a screen over the panel with a search bar on top and every launchable app below. Tapping one opens
 * it on the panel's side of the split (through OpenInPanel), so the map stays visible.
 */
final class AppsGrid extends LinearLayout {

    private final Context ctx;
    private final boolean dark;
    private final Runnable onClose;
    private final List<String[]> all;
    private final List<String[]> shown = new ArrayList<>();
    private final Map<String, Drawable> icons = new HashMap<>();
    private final BaseAdapter adapter;
    private final TextView empty;

    AppsGrid(Context ctx, boolean dark, Runnable onClose) {
        super(ctx);
        this.ctx = ctx;
        this.dark = dark;
        this.onClose = onClose;
        setOrientation(VERTICAL);
        setBackground(new GlassBackground(dark));
        int pad = Glass.dp(ctx, 16);
        setPadding(pad, pad, pad, pad);
        setClickable(true); // swallow touches so the panel below is not pressed through it

        all = MapApps.order(MapAppRepo.installed(ctx));
        all.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
        shown.addAll(all);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new IconView(ctx, Icon.BACK, "Back", dark, 44, IconView.Style.NORMAL, onClose));
        TextView title = label("Apps", 20, true, PanelStyle.fg(dark));
        LayoutParams tp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = Glass.dp(ctx, 12);
        head.addView(title, tp);
        addView(head);

        final EditText q = new EditText(ctx);
        q.setHint("Search apps");
        q.setSingleLine();
        q.setInputType(InputType.TYPE_CLASS_TEXT);
        q.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        q.setTextColor(PanelStyle.fg(dark));
        q.setHintTextColor(PanelStyle.dim(dark));
        q.setBackground(Glass.button(ctx, dark, false, false, 16));
        int qp = Glass.dp(ctx, 14);
        q.setPadding(qp, qp, qp, qp);
        LayoutParams ql = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ql.topMargin = Glass.dp(ctx, 12);
        addView(q, ql);

        empty = label("No app matches that.", 14, false, PanelStyle.dim(dark));
        empty.setVisibility(GONE);
        LayoutParams el = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        el.topMargin = Glass.dp(ctx, 14);
        addView(empty, el);

        GridView grid = new GridView(ctx);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Glass.dp(ctx, 92));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(Glass.dp(ctx, 10));
        grid.setVerticalScrollBarEnabled(false);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int i) { return shown.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View convert, ViewGroup parent) { return cell(shown.get(i)); }
        };
        grid.setAdapter(adapter);
        LayoutParams gl = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        gl.topMargin = Glass.dp(ctx, 10);
        addView(grid, gl);

        q.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                shown.clear();
                shown.addAll(AppFilter.filter(all, s.toString()));
                empty.setVisibility(shown.isEmpty() ? VISIBLE : GONE);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private View cell(final String[] app) {
        LinearLayout c = new LinearLayout(ctx);
        c.setOrientation(VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Glass.dp(ctx, 6);
        c.setPadding(pad, pad, pad, pad);
        c.setBackground(Glass.pressOverlay(ctx, dark, 12));
        c.setClickable(true);
        c.setContentDescription(app[1]);
        c.setOnClickListener(v -> open(app));
        android.widget.ImageView icon = new android.widget.ImageView(ctx);
        icon.setImageDrawable(iconOf(app[0]));
        c.addView(icon, new LayoutParams(Glass.dp(ctx, 48), Glass.dp(ctx, 48)));
        TextView t = label(app[1], 12, false, PanelStyle.fg(dark));
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams tl = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tl.topMargin = Glass.dp(ctx, 4);
        c.addView(t, tl);
        return c;
    }

    private Drawable iconOf(String pkg) {
        Drawable d = icons.get(pkg);
        if (d == null) {
            try { d = ctx.getPackageManager().getApplicationIcon(pkg); } catch (Exception e) { d = null; }
            if (d != null) icons.put(pkg, d);
        }
        return d;
    }

    private void open(String[] app) {
        int[] at = new int[2];
        getLocationOnScreen(at);
        OpenInPanel.request(ctx, app[0], app[1], new int[]{at[0], at[1], getWidth(), getHeight()});
        onClose.run();
    }

    private TextView label(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTypeface(android.graphics.Typeface.DEFAULT, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        t.setTextColor(color);
        return t;
    }
}
