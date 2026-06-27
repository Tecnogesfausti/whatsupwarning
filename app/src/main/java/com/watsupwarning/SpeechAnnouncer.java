package com.watsupwarning;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

final class SpeechAnnouncer {
    private static TextToSpeech tts;
    private static boolean ready;

    private SpeechAnnouncer() {
    }

    static void speak(Context context, String message) {
        Context appContext = context.getApplicationContext();
        AudioManager audio = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        int originalVolume = -1;
        if (audio != null) {
            originalVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int target = Math.max(originalVolume, Math.max(1, (int) (max * 0.7f)));
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
            requestFocus(audio);
        }

        int volumeToRestore = originalVolume;
        Runnable say = () -> {
            if (tts == null || !ready) {
                RuleStore.recordEvent(appContext, "Speech skipped: TTS not ready");
                return;
            }
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "watsupwarning-ha");
            if (audio != null && volumeToRestore >= 0) {
                new Thread(() -> {
                    try {
                        Thread.sleep(6500);
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volumeToRestore, 0);
                    } catch (InterruptedException ignored) {
                    }
                }, "restore-volume").start();
            }
        };

        if (tts == null) {
            tts = new TextToSpeech(appContext, status -> {
                ready = status == TextToSpeech.SUCCESS;
                if (ready) {
                    tts.setLanguage(new Locale("es", "ES"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build());
                    }
                    say.run();
                } else {
                    RuleStore.recordEvent(appContext, "Speech failed: TTS init error");
                }
            });
        } else {
            say.run();
        }
    }

    private static void requestFocus(AudioManager audio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .build();
            audio.requestAudioFocus(request);
        } else {
            audio.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }
}
