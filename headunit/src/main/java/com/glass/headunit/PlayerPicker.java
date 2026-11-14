package com.glass.headunit;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Shown over the panel (or in the bubble's overlay): pick a player from round-cornered cards with their app icons, or
 * tap a Quick pick (a pinned station, song or playlist) or a recent song. Search sits at the end, since it needs
 * typing. Tapping a card starts that player and keeps the list open so the next step is one tap away.
 */
final class PlayerPicker extends LinearLayout {

    private final Context ctx;
    private final MediaFollower follower;
    private final PlayerSwitcher switcher;
    private final Runnable onClose;
    private final boolean dark;
    private final LinearLayout cards, moreCards, content;
    private final Button moreToggle, openBtn;
    private String selectedPkg, selectedLabel = "";
    private boolean moreOpen;
    private boolean searchOpen;   // the search field is showing in place of the lists

    PlayerPicker(Context ctx, MediaFollower followerOrNull, boolean dark, Runnable onClose) {
        super(ctx);
        this.ctx = ctx;
        this.follower = followerOrNull;
        this.switcher = new PlayerSwitcher(ctx, followerOrNull);
        this.onClose = onClose;
        this.dark = dark;
        setOrientation(VERTICAL);
        setBackground(new GlassBackground(dark));
        int pad = Glass.dp(ctx, 16);
        setPadding(pad, pad, pad, pad);
        setClickable(true); // swallow touches so the panel below is not pressed through it

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new IconView(ctx, Icon.BACK, "Back", dark, 44, IconView.Style.NORMAL, () -> { if (!handleBack()) onClose.run(); }));
        TextView title = label("Players", 20, true, PanelStyle.fg(dark));
        LayoutParams tp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = Glass.dp(ctx, 12);
        head.addView(title, tp);
        addView(head);

        cards = new LinearLayout(ctx);
        cards.setOrientation(HORIZONTAL);
        HorizontalScrollView cardScroll = new HorizontalScrollView(ctx);
        cardScroll.setHorizontalScrollBarEnabled(false);
        cardScroll.addView(cards);
        LayoutParams cl = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cl.topMargin = Glass.dp(ctx, 12);
        addView(cardScroll, cl);

        moreToggle = pill("More apps", false, () -> { moreOpen = !moreOpen; refreshPlayers(); });
        moreCards = new LinearLayout(ctx);
        moreCards.setOrientation(HORIZONTAL);
        HorizontalScrollView moreScroll = new HorizontalScrollView(ctx);
        moreScroll.setHorizontalScrollBarEnabled(false);
        moreScroll.addView(moreCards);
        LinearLayout moreBox = new LinearLayout(ctx);
        moreBox.setOrientation(VERTICAL);
        moreBox.addView(moreToggle, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        moreBox.addView(moreScroll, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LayoutParams ml = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ml.topMargin = Glass.dp(ctx, 8);
        addView(moreBox, ml);

        // the real app, for choosing a playlist or album in its own screens (works for any player, even ones that keep their library closed)
        openBtn = Glass.textButton(ctx, "", dark, this::openApp);
        LayoutParams ol = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ol.topMargin = Glass.dp(ctx, 10);
        addView(openBtn, ol);

        content = new LinearLayout(ctx);
        content.setOrientation(VERTICAL);
        ScrollView sv = new ScrollView(ctx);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(content);
        LayoutParams sl = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        sl.topMargin = Glass.dp(ctx, 10);
        addView(sv, sl);

        if (follower != null && follower.controller() != null) selectedPkg = follower.controller().getPackageName();
        refreshPlayers();
        showMain();
    }

    /** Back closes the search field first; false when there is nothing to close (so the caller can close the picker). */
    boolean handleBack() {
        if (!searchOpen) return false;
        showMain();
        return true;
    }

    // ---- players ----

    private void refreshPlayers() {
        List<String[]> all = Proxy.players(ctx);
        PlayerList.Split split = PlayerList.split(all, Proxy.activePackages(ctx));
        if (selectedPkg == null && !split.main.isEmpty()) selectedPkg = split.main.get(0)[0];
        cards.removeAllViews();
        moreCards.removeAllViews();
        if (split.main.isEmpty() && split.more.isEmpty()) {
            cards.addView(label("No media apps found on this unit.", 15, false, PanelStyle.dim(dark)));
        }
        for (String[] p : split.main) cards.addView(card(p[0], p[1]));
        for (String[] p : split.more) {
            if (p[0].equals(selectedPkg)) selectedLabel = p[1];
            moreCards.addView(card(p[0], p[1]));
        }
        for (String[] p : split.main) if (p[0].equals(selectedPkg)) selectedLabel = p[1];
        ((View) moreToggle.getParent()).setVisibility(split.more.isEmpty() ? GONE : VISIBLE);
        moreCards.setVisibility(moreOpen ? VISIBLE : GONE);
        ((View) moreCards.getParent()).setVisibility(moreOpen ? VISIBLE : GONE);
        moreToggle.setText(moreOpen ? "Fewer apps" : "More apps (" + split.more.size() + ")");
        openBtn.setText(selectedPkg == null ? "" : "Open " + selectedLabel + " to choose music");
        openBtn.setVisibility(selectedPkg == null ? GONE : VISIBLE);
        ((View) moreToggle.getParent()).setVisibility(split.more.isEmpty() ? GONE : VISIBLE);
    }

    private View card(final String pkg, final String name) {
        boolean selected = pkg.equals(selectedPkg);
        LinearLayout c = new LinearLayout(ctx);
        c.setOrientation(VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Glass.dp(ctx, 10);
        c.setPadding(pad, pad, pad, pad);
        c.setBackground(Glass.button(ctx, dark, false, selected, 16));
        c.setClickable(true);
        c.setContentDescription(name);
        c.setOnClickListener(v -> selectPlayer(pkg, name));
        c.addView(appIcon(pkg, 44), new LayoutParams(Glass.dp(ctx, 44), Glass.dp(ctx, 44)));
        TextView t = label(name, 12, selected, PanelStyle.fg(dark));
        t.setGravity(Gravity.CENTER);
        t.setSingleLine();
        t.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams tl = new LayoutParams(Glass.dp(ctx, 92), LayoutParams.WRAP_CONTENT);
        tl.topMargin = Glass.dp(ctx, 6);
        c.addView(t, tl);
        LayoutParams lp = new LayoutParams(Glass.dp(ctx, 114), LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Glass.dp(ctx, 8);
        c.setLayoutParams(lp);
        return c;
    }

    /** Tapping a card starts that player and keeps the list open, so searching in it is the next tap. */
    private void selectPlayer(String pkg, String name) {
        boolean changed = !pkg.equals(selectedPkg);
        selectedPkg = pkg;
        selectedLabel = name;
        if (changed || follower == null || follower.controller() == null || !pkg.equals(follower.controller().getPackageName())) switcher.switchTo(pkg);
        refreshPlayers();
        showMain();
    }

    /**
     * Opens the selected player itself on the panel's side of the split (the map stays where it is); the Back key or
     * the floating button brings the panel back. From the floating bubble the panel does the opening (OpenInPanel).
     */
    private void openApp() {
        if (selectedPkg == null) return;
        int[] at = new int[2];
        getLocationOnScreen(at);
        OpenInPanel.request(ctx, selectedPkg, selectedLabel, new int[]{at[0], at[1], getWidth(), getHeight()});
        onClose.run();
    }

    // ---- Quick picks, Recent and Search ----

    /** Quick picks first, then Recent, then a Search button: everything reachable with one tap while driving. */
    private void showMain() {
        searchOpen = false;
        content.removeAllViews();
        content.addView(label("Quick picks", 15, true, PanelStyle.dim(dark)));
        List<Pin> pins = Pins.all(ctx);
        if (pins.isEmpty()) {
            content.addView(label("Hold the song on the panel, or share a playlist to Glass Panel, to pin it here.", 14, false, PanelStyle.dim(dark)), gapParams(8));
        } else {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(HORIZONTAL);
            for (Pin p : pins) row.addView(tile(p), tileParams());
            HorizontalScrollView hs = new HorizontalScrollView(ctx);
            hs.setHorizontalScrollBarEnabled(false);
            hs.addView(row);
            content.addView(hs, gapParams(8));
        }
        content.addView(label("Recent", 15, true, PanelStyle.dim(dark)), gapParams(18));
        List<History.Entry> recent = History.recent(ctx, 25);
        if (recent.isEmpty()) content.addView(label("Songs you play will show up here.", 14, false, PanelStyle.dim(dark)), gapParams(8));
        long now = System.currentTimeMillis();
        for (final History.Entry e : recent) {
            String sub = Proxy.label(ctx, e.pkg) + " · " + AgoText.of(e.last, now);
            content.addView(recentRow(e.title, (e.artist.isEmpty() ? "" : e.artist + "  ·  ") + sub, e.pkg,
                    () -> { switcher.playSong(e.pkg, e.title, e.artist); onClose.run(); }), gapParams(10));
        }
        Button search = Glass.textButton(ctx, "Search", dark, this::showSearch);
        search.setMinHeight(Glass.dp(ctx, 56));
        content.addView(search, gapParams(18));
    }

    /** A Recent row, about twice the old height so it is easy to hit while driving: player icon, title, details, Play. */
    private View recentRow(String title, String subtitle, String pkg, Runnable onClick) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Glass.dp(ctx, 16);
        r.setPadding(pad, pad, pad, pad);
        r.setMinimumHeight(Glass.dp(ctx, 112));
        r.setBackground(Glass.button(ctx, dark, false, false, 16));
        r.setClickable(true);
        r.setOnClickListener(v -> onClick.run());
        LayoutParams ip = new LayoutParams(Glass.dp(ctx, 48), Glass.dp(ctx, 48));
        ip.rightMargin = Glass.dp(ctx, 14);
        r.addView(appIcon(pkg, 48), ip);
        LinearLayout words = new LinearLayout(ctx);
        words.setOrientation(VERTICAL);
        TextView t = label(title, 19, true, PanelStyle.fg(dark));
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(t);
        TextView sub = label(subtitle, 15, false, PanelStyle.dim(dark));
        sub.setSingleLine();
        sub.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(sub);
        r.addView(words, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        LayoutParams pl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        pl.leftMargin = Glass.dp(ctx, 10);
        r.addView(new IconView(ctx, Icon.PLAY, "Play", dark, 44, IconView.Style.NORMAL, onClick), pl);
        return r;
    }

    private LayoutParams tileParams() {
        LayoutParams lp = new LayoutParams(Glass.dp(ctx, 96), LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Glass.dp(ctx, 10);
        return lp;
    }

    /** A Quick pick tile: cover (or the player's icon), a small player badge, the name and what kind of thing it is. */
    private View tile(final Pin p) {
        boolean ok = installed(p.pkg);
        LinearLayout t = new LinearLayout(ctx);
        t.setOrientation(VERTICAL);
        int pad = Glass.dp(ctx, 8);
        t.setPadding(pad, pad, pad, pad);
        t.setBackground(Glass.button(ctx, dark, false, false, 14));
        t.setClickable(true);
        t.setAlpha(ok ? 1f : 0.4f);
        android.widget.FrameLayout art = new android.widget.FrameLayout(ctx);
        android.graphics.Bitmap cover = Pins.cover(ctx, p);
        if (cover != null) {
            ImageView iv = new ImageView(ctx);
            iv.setImageBitmap(cover);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            final float radius = Glass.dp(ctx, 10);
            iv.setClipToOutline(true);
            iv.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
            });
            art.addView(iv, new android.widget.FrameLayout.LayoutParams(-1, -1));
            android.widget.FrameLayout.LayoutParams bl = new android.widget.FrameLayout.LayoutParams(Glass.dp(ctx, 22), Glass.dp(ctx, 22), Gravity.END | Gravity.BOTTOM);
            bl.rightMargin = bl.bottomMargin = Glass.dp(ctx, 4);
            art.addView(appIcon(p.pkg, 22), bl);
        } else {
            art.addView(appIcon(p.pkg, 64), new android.widget.FrameLayout.LayoutParams(Glass.dp(ctx, 64), Glass.dp(ctx, 64), Gravity.CENTER));
        }
        t.addView(art, new LayoutParams(LayoutParams.MATCH_PARENT, Glass.dp(ctx, 64)));
        TextView name = label(p.name, 14, true, PanelStyle.fg(dark));
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        t.addView(name, gapParams(6));
        t.addView(label(p.kindLabel(), 12, false, PanelStyle.dim(dark)));
        t.setOnClickListener(v -> {
            if (!installed(p.pkg)) { Spike.log("pin: " + p.pkg + " is not installed"); return; }
            switcher.playPin(p);
            onClose.run();
        });
        t.setOnLongClickListener(v -> { showPinMenu(p); return true; });
        return t;
    }

    /** Held a tile: move it or unpin it. */
    private void showPinMenu(final Pin p) {
        content.removeAllViews();
        content.addView(label(p.name, 18, true, PanelStyle.fg(dark)));
        content.addView(bigButton("Move left", () -> { Pins.save(ctx, Pins.move(Pins.all(ctx), p.key(), -1)); showMain(); }), gapParams(12));
        content.addView(bigButton("Move right", () -> { Pins.save(ctx, Pins.move(Pins.all(ctx), p.key(), +1)); showMain(); }), gapParams(10));
        content.addView(bigButton("Unpin", () -> { Pins.unpin(ctx, p); showMain(); }), gapParams(10));
        content.addView(bigButton("Cancel", this::showMain), gapParams(10));
    }

    private Button bigButton(String text, Runnable r) {
        Button b = Glass.textButton(ctx, text, dark, r);
        b.setMinHeight(Glass.dp(ctx, 56));
        return b;
    }

    private boolean installed(String pkg) {
        try { return ctx.getPackageManager().getApplicationInfo(pkg, 0).enabled; } catch (Exception e) { return false; }
    }

    private void showSearch() {
        searchOpen = true;
        content.removeAllViews();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final EditText q = new EditText(ctx);
        q.setHint("Song or artist");
        q.setSingleLine();
        q.setInputType(InputType.TYPE_CLASS_TEXT);
        q.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        q.setTextColor(PanelStyle.fg(dark));
        q.setHintTextColor(PanelStyle.dim(dark));
        q.setBackground(Glass.button(ctx, dark, false, false, 16));
        int pad = Glass.dp(ctx, 14);
        q.setPadding(pad, pad, pad, pad);
        row.addView(q, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        final Runnable go = () -> {
            String query = q.getText().toString().trim();
            if (query.isEmpty() || selectedPkg == null) return;
            switcher.playSong(selectedPkg, query, "");
            onClose.run();
        };
        IconView search = new IconView(ctx, Icon.SEARCH, "Search and play", dark, 48, IconView.Style.MAIN, go);
        LayoutParams sp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        sp.leftMargin = Glass.dp(ctx, 10);
        row.addView(search, sp);
        q.setOnEditorActionListener((v, actionId, e) -> { go.run(); return true; });
        content.addView(row);
        content.addView(label(selectedPkg == null ? "Pick a player above." : "Plays in " + selectedLabel + ". Say the song or artist with the keyboard's microphone.", 13, false, PanelStyle.dim(dark)),
                gapParams(10));
        content.addView(bigButton("Back to Quick picks", this::showMain), gapParams(14));
    }

    // ---- small view helpers ----

    private LayoutParams gapParams(int topDp) {
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Glass.dp(ctx, topDp);
        return lp;
    }

    private TextView label(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setTextColor(color);
        return t;
    }

    /** A rounded glass pill; lit with the accent when selected. */
    private Button pill(String text, boolean selected, Runnable r) {
        Button b = new Button(ctx);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(selected ? PanelStyle.accent(dark) : PanelStyle.fg(dark));
        b.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        b.setBackground(Glass.button(ctx, dark, false, selected, 22));
        b.setMinHeight(Glass.dp(ctx, 44));
        b.setMinimumHeight(Glass.dp(ctx, 44));
        b.setPadding(Glass.dp(ctx, 18), 0, Glass.dp(ctx, 18), 0);
        b.setOnClickListener(v -> r.run());
        return b;
    }

    /** A glass row: optional app icon, a title and a quieter second line. */
    private View row(String title, String subtitle, String iconPkg, Runnable onClick) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Glass.dp(ctx, 12);
        r.setPadding(pad, pad, pad, pad);
        r.setBackground(Glass.button(ctx, dark, false, false, 14));
        r.setClickable(true);
        r.setOnClickListener(v -> onClick.run());
        if (iconPkg != null) {
            LayoutParams ip = new LayoutParams(Glass.dp(ctx, 36), Glass.dp(ctx, 36));
            ip.rightMargin = Glass.dp(ctx, 12);
            r.addView(appIcon(iconPkg, 36), ip);
        }
        LinearLayout words = new LinearLayout(ctx);
        words.setOrientation(VERTICAL);
        TextView t = label(title, 16, true, PanelStyle.fg(dark));
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = label(subtitle, 13, false, PanelStyle.dim(dark));
            s.setSingleLine();
            s.setEllipsize(TextUtils.TruncateAt.END);
            words.addView(s);
        }
        r.addView(words, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    private View appIcon(String pkg, int sizeDp) {
        ImageView iv = new ImageView(ctx);
        Drawable d = null;
        try { d = ctx.getPackageManager().getApplicationIcon(pkg); } catch (Exception ignored) { }
        iv.setImageDrawable(d);
        final float radius = Glass.dp(ctx, sizeDp * 0.24f);
        iv.setClipToOutline(true);
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
        });
        return iv;
    }
}
