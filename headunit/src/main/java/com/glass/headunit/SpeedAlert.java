package com.glass.headunit;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

/**
 * The speed alert sound: a short run of beeps made by the system (no sound file). It plays on the media volume, like a
 * navigation prompt: head units often keep the alarm volume at zero or do not send it to the car's speakers, so a beep
 * there is silent. Music is asked to dip while it plays.
 */
final class SpeedAlert {

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());

    SpeedAlert(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    private volatile boolean playing;

    /** True from raising the volume for the beep until it is put back: the speed volume leaves the volume alone meanwhile. */
    boolean playing() { return playing; }

    void play() {
        final int stream = BeepVolume.stream(Settings.of(ctx).beepSeparate());
        playing = stream == AudioManager.STREAM_MUSIC;   // only then is the music's volume raised for a moment
        final AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        final AudioFocusRequest focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .build();
        try { am.requestAudioFocus(focus); } catch (Exception ignored) { }
        int vol = am.getStreamVolume(stream), max = am.getStreamMaxVolume(stream);
        final int before = vol;
        final int raisedTo = BeepVolume.target(vol, max, Settings.of(ctx).beepVolume());
        if (raisedTo > vol) {
            try {
                am.setStreamVolume(stream, raisedTo, 0);
                Spike.log("speed alert: " + (stream == AudioManager.STREAM_ALARM ? "alarm" : "media") + " volume raised from " + vol + " to " + raisedTo);
                vol = raisedTo;
            } catch (Exception e) { Spike.log("speed alert: could not raise the volume (" + e.getClass().getSimpleName() + ")"); }
        }
        final boolean raised = vol > before;
        if (vol == 0) Spike.log("speed alert: the media volume is at zero, so the beep cannot be heard");
        try {
            final ToneGenerator tone = new ToneGenerator(stream, 100);
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800);
            Spike.log("speed alert: sounded (" + (stream == AudioManager.STREAM_ALARM ? "alarm" : "media") + " volume " + vol + "/" + max + ")");
            handler.postDelayed(() -> {
                try {
                    putBack(am, stream, raised, before, raisedTo);   // before the music comes back up, so it is never loud for a moment
                    playing = false;
                    try { am.abandonAudioFocusRequest(focus); } catch (Exception ignored) { }
                    tone.release();
                } catch (Exception e) { Spike.log("speed alert: could not tidy up after the beep (" + e.getClass().getSimpleName() + ")"); }
                finally { playing = false; }
            }, 1500);
        } catch (RuntimeException e) {
            Spike.log("speed alert: the unit could not make the sound (" + e.getClass().getSimpleName() + ")");
            putBack(am, stream, raised, before, raisedTo);
            playing = false;
            try { am.abandonAudioFocusRequest(focus); } catch (Exception ignored) { }
        }
    }

    /** Sets the volume the beep used back to where it was, unless the driver changed it meanwhile. Never leaves it raised by mistake. */
    private static void putBack(AudioManager am, int stream, boolean raised, int before, int raisedTo) {
        if (!raised) return;
        try {
            int now = am.getStreamVolume(stream);
            int level = BeepVolume.restoreLevel(true, before, raisedTo, now);
            if (level >= 0) {
                am.setStreamVolume(stream, level, 0);
                Spike.log("speed alert: volume put back to " + level);
            } else Spike.log("speed alert: volume left at " + now + ", as the driver set it");
        } catch (Exception e) {
            Spike.log("speed alert: could not put the volume back (" + e.getClass().getSimpleName() + "), setting it to " + before);
            try { am.setStreamVolume(stream, before, 0); } catch (Exception ignored) { }
        }
    }
}
