package kr.go.namwon.seniorcenter.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsHelper {
    private static final String PREF_NAME = "NamwonPrefs";
    private static SharedPreferences prefs;

    private PrefsHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (prefs == null) new PrefsHelper(context);
    }

    public static void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        return prefs.getString(key, defValue);
    }

    public static void clear() {
        prefs.edit().clear().apply();
    }
}
