package kr.go.namwon.seniorcenter.app.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

public class Ringer {

    private static final String TAG = "Ringer";
    private static final String PREF_NAME = "ringer_prefs";
    private static final String KEY_RINGTONE_URI = "ringtone_uri";
    private static Ringtone currentRingtone;

    public static void playBeep(Context context) {
        try {
            if (currentRingtone != null && currentRingtone.isPlaying()) {
                currentRingtone.stop();
            }

            Uri sound = getSavedRingtoneUri(context);
            currentRingtone = RingtoneManager.getRingtone(context, sound);

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            currentRingtone.setAudioAttributes(attrs);
            currentRingtone.play();
        } catch (Exception e) {
            Log.e(TAG, "beep failed", e);
        }
    }

    public static Uri getSavedRingtoneUri(Context context) {
        String uriStr = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_RINGTONE_URI, null);

        if (uriStr != null) return Uri.parse(uriStr);
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    public static void saveRingtoneUri(Context context, Uri uri) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RINGTONE_URI, uri != null ? uri.toString() : null)
                .apply();
    }
}