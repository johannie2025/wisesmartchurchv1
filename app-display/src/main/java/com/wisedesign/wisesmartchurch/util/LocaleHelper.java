package com.wisedesign.elitecastpro.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_KEY = "app_locale";
    private static final String PREFS_NAME = "elitecast_prefs";

    public static void applyLocale(Context context) {
        String lang = getSavedLocale(context);
        setLocale(context, lang);
    }

    public static void setLocale(Context context, String lang) {
        saveLocale(context, lang);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());
    }

    public static String getSavedLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getString(PREF_KEY, "fr"); // Français par défaut
    }

    private static void saveLocale(Context context, String lang) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_KEY, lang).apply();
    }
}
