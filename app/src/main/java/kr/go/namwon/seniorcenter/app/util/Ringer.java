package kr.go.namwon.seniorcenter.app.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

public class Ringer {

    private static final String TAG = "Ringer";
    public static void playBeep(Context context) {
        try {
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, sound);

            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)          // 핵심
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            r.setAudioAttributes(attrs);
            r.play();
        } catch (Exception e) {
            Log.e(TAG, "beep failed", e);
        }
    }
}
