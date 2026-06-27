package com.watsupwarning;

import android.content.Context;
import android.content.SharedPreferences;

final class HomeAssistantSettings {
    private static final String PREFS = "watsupwarning_home_assistant";
    private static final String BASE_URL = "base_url";
    private static final String TOKEN = "token";

    private HomeAssistantSettings() {
    }

    static String getBaseUrl(Context context) {
        return prefs(context).getString(BASE_URL, "");
    }

    static String getToken(Context context) {
        return prefs(context).getString(TOKEN, "");
    }

    static void save(Context context, String baseUrl, String token) {
        prefs(context).edit()
                .putString(BASE_URL, normalizeBaseUrl(baseUrl))
                .putString(TOKEN, token == null ? "" : token.trim())
                .apply();
    }

    static String resolveUrl(Context context, String actionUrl) {
        String value = actionUrl == null ? "" : actionUrl.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        String baseUrl = getBaseUrl(context);
        if (baseUrl.isEmpty() || value.isEmpty()) {
            return value;
        }
        String path = value.startsWith("/") ? value : "/" + value;
        return baseUrl + path;
    }

    private static String normalizeBaseUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty() || value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "http://" + value;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
