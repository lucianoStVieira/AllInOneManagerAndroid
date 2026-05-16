package com.allinonemanager.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public final class AppLanguage {
    public static final String SYSTEM_DEFAULT = "";

    private static final String PREFS_NAME = "app_language";
    private static final String KEY_LANGUAGE_TAG = "language_tag";

    private AppLanguage() {
    }

    public static Context apply(Context context) {
        String languageTag = selectedLanguageTag(context);
        if (languageTag.isEmpty()) {
            return context;
        }

        Locale locale = Locale.forLanguageTag(languageTag);
        if (locale.getLanguage().isEmpty()) {
            return context;
        }

        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    public static Locale currentLocale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    public static String selectedLanguageTag(Context context) {
        return prefs(context).getString(KEY_LANGUAGE_TAG, SYSTEM_DEFAULT);
    }

    public static void saveSelectedLanguageTag(Context context, String languageTag) {
        prefs(context).edit()
                .putString(KEY_LANGUAGE_TAG, supportedLanguageTag(languageTag))
                .apply();
    }

    public static int selectedLanguageIndex(Context context, String[] languageTags) {
        String selected = selectedLanguageTag(context);
        for (int i = 0; i < languageTags.length; i++) {
            if (selected.equals(languageTags[i])) {
                return i;
            }
        }

        return 0;
    }

    private static String supportedLanguageTag(String languageTag) {
        if (languageTag == null || languageTag.trim().isEmpty()) {
            return SYSTEM_DEFAULT;
        }

        String normalized = languageTag.trim();
        if ("en".equals(normalized) || "pt".equals(normalized)) {
            return normalized;
        }

        return SYSTEM_DEFAULT;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
