package com.glass.headunit;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends the queued messages to the Home Assistant webhook, oldest first, on one background thread. Each message is
 * saved on the unit before sending and removed only when Home Assistant accepts it, so no signal (or the unit closing
 * the app) only delays it; a message keeps its own "time" through every retry, so Home Assistant can drop a repeat.
 * A failed send is retried with a growing wait, and again at once when the network returns, the unit wakes or the app
 * starts. Nothing happens unless the user turned it on and saved an https address.
 */
final class HaSender {

    interface Result { void done(String message); }

    private static HaSender instance;

    private final Context app;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ha-sender");
        t.setDaemon(true);
        return t;
    });
    private final File file;
    private HaQueue queue;
    private int attempt;
    private boolean scheduled;
    private long notBefore;   // a refusal waits long before the next try; a reconnect, a wake or a settings change clears it
    private static final long AWAKE_MS = 120_000L;   // the unit stays awake this long (at most) while a drive's summary is on its way
    private volatile boolean noSignal;
    private volatile int waiting;

    private HaSender(Context c) {
        app = c.getApplicationContext();
        file = new File(app.getFilesDir(), "ha-queue.txt");
        worker.execute(() -> {
            try { queue = HaQueue.fromText(file.exists() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : ""); }
            catch (Exception e) { queue = new HaQueue(); }
            waiting = queue.size();
        });
        try {
            app.getSystemService(ConnectivityManager.class).registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network n) { kick(); }
            });
        } catch (Exception e) { Spike.log("ha: could not watch the network (" + e.getClass().getSimpleName() + ")"); }
    }

    static synchronized HaSender get(Context c) {
        if (instance == null) instance = new HaSender(c);
        return instance;
    }

    private boolean enabled() {
        Settings s = Settings.of(app);
        return s.haOn() && HaRules.validUrl(s.haUrl());
    }

    /** Queues a message (saved to the file first) and starts sending; nothing when the link is off. */
    void enqueue(JSONObject msg) {
        if (!enabled()) return;
        final String json = msg.toString();
        worker.execute(() -> {
            queue.add(json, System.currentTimeMillis());
            queue.prune(System.currentTimeMillis());
            save();
            String type = msg.optString("type");
            if (type.equals("car_stopped")) Awake.hold(app, "ha-summary", AWAKE_MS);   // the unit may be about to sleep
            Spike.log("ha: queued " + type + (type.equals("position") ? "" : " (time " + msg.optString("time") + ")") + ", " + queue.size() + " waiting");
            sendLoop();
        });
    }

    /** Settings changed (turned on, new address): clear a refusal and try again now. */
    void retryNow() { worker.execute(() -> { attempt = 0; notBefore = 0; Settings.of(app).setHaError(""); sendLoop(); }); }

    /** The network came back, the unit woke up or the app started: whatever is waiting goes now, whatever the backoff says. */
    void kick() { worker.execute(() -> { attempt = 0; notBefore = 0; noSignal = false; sendLoop(); }); }

    private void save() {
        waiting = queue.size();
        try { Files.write(file.toPath(), queue.toText().getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { Spike.log("ha: could not save the queue (" + e.getClass().getSimpleName() + ")"); }
    }

    private int sentSinceSave;

    private void sendLoop() {
        try { drain(); } finally { if (sentSinceSave > 0) { save(); sentSinceSave = 0; } }
    }

    private void drain() {
        if (queue == null || !enabled()) return;
        Settings s = Settings.of(app);
        if (System.currentTimeMillis() < notBefore) return;   // after a refusal: the slow retry decides
        while (queue.peek() != null) {
            String type = queue.peekType(), time = queue.peekTime();
            long queuedAt = queue.peekQueuedAt();
            int code = post(s.haUrl(), queue.peek());
            if (code >= 200 && code < 300) {
                queue.removeFirst();
                waiting = queue.size();
                if (++sentSinceSave >= 50) { save(); sentSinceSave = 0; }   // not the whole file for every message
                attempt = 0;
                notBefore = 0;
                noSignal = false;
                if (!s.haError().isEmpty()) s.setHaError("");   // it works again
                s.setHaLastSentAt(System.currentTimeMillis());
                if (!"position".equals(type)) Spike.log("ha: delivered " + type + " (time " + time + "), queued " + (System.currentTimeMillis() - queuedAt) / 1000 + " s ago, " + waiting + " waiting");
                continue;
            }
            noSignal = code < 0;
            if (HaRules.stopOn(code)) {
                // not for good: a proxy or tunnel can refuse while Home Assistant restarts. Keep everything and try again slowly.
                s.setHaError("Home Assistant said " + code + ". Check the address. Trying again every " + HaRules.REFUSED_RETRY_MS / 60_000 + " min.");
                notBefore = System.currentTimeMillis() + HaRules.REFUSED_RETRY_MS;
            }
            if (!scheduled) {
                long wait = HaRules.retryDelayMs(code, attempt++);
                scheduled = true;
                worker.schedule(() -> { scheduled = false; sendLoop(); }, wait, TimeUnit.MILLISECONDS);
                Spike.log("ha: could not send " + type + (code < 0 ? " (no connection)" : " (code " + code + ")") + ", " + queue.size() + " waiting, trying again in " + wait / 1000 + " s");
            }
            return;
        }
        Awake.release("ha-summary");   // everything is delivered: the unit may sleep
    }

    /** The HTTP status, or -1 when Home Assistant couldn't be reached. */
    private static int post(String url, String json) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(10_000);
            c.setReadTimeout(10_000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent", "GlassPanel");
            try (OutputStream o = c.getOutputStream()) { o.write(json.getBytes(StandardCharsets.UTF_8)); }
            return c.getResponseCode();
        } catch (Exception e) {
            return -1;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** The test button: posts a test message straight away (not through the queue) and says what happened. */
    void test(final Result result) {
        final Settings s = Settings.of(app);
        final String url = s.haUrl();
        if (!HaRules.validUrl(url)) { result.done("Enter an address that starts with https:// first."); return; }
        worker.execute(() -> {
            int code = post(url, HaMessage.test(s.haCarName(), System.currentTimeMillis()).toString());
            String msg = code >= 200 && code < 300 ? "Home Assistant answered. Check that the test notification appeared there: a wrong webhook id is also answered without an error."
                    : code < 0 ? "Couldn't reach Home Assistant. Check the address and the unit's internet."
                    : "Home Assistant said " + code + ". Check the address.";
            Spike.log("ha: test -> " + (code < 0 ? "no connection" : "code " + code));
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> result.done(msg));
        });
    }

    /** "Last sent 2 min ago · 0 waiting", "14 waiting · no signal", or why it stopped. */
    String status() {
        Settings s = Settings.of(app);
        if (!s.haError().isEmpty()) return s.haError();
        long last = s.haLastSentAt();
        String sent = last == 0 ? "Nothing sent yet" : "Last sent " + AgoText.of(last, System.currentTimeMillis());
        return sent + " · " + waiting + " waiting" + (noSignal && waiting > 0 ? " · no signal" : "");
    }
}
