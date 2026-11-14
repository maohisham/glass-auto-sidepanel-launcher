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

    void play() {
        final AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        final AudioFocusRequest focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .build();
        try { am.requestAudioFocus(focus); } catch (Exception ignored) { }
        int vol = am.getStreamVolume(AudioManager.STREAM_MUSIC), max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (vol == 0) Spike.log("speed alert: the media volume is at zero, so the beep cannot be heard");
        try {
            final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800);
            Spike.log("speed alert: sounded (media volume " + vol + "/" + max + ")");
            handler.postDelayed(() -> {
                tone.release();
                try { am.abandonAudioFocusRequest(focus); } catch (Exception ignored) { }
            }, 1500);
        } catch (RuntimeException e) {
            Spike.log("speed alert: the unit could not make the sound (" + e.getClass().getSimpleName() + ")");
            try { am.abandonAudioFocusRequest(focus); } catch (Exception ignored) { }
        }
    }
}
