package com.watsupwarning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class Rule {
    static final String ACTION_LOG = "log";
    static final String ACTION_HOME_ASSISTANT = "home_assistant";

    final String id;
    final String name;
    final List<String> keywords;
    final String actionType;
    final String actionUrl;
    final String bearerToken;
    final boolean enabled;

    Rule(String id, String name, List<String> keywords, String actionType, String actionUrl, String bearerToken, boolean enabled) {
        this.id = id;
        this.name = name;
        this.keywords = keywords;
        this.actionType = actionType;
        this.actionUrl = actionUrl;
        this.bearerToken = bearerToken;
        this.enabled = enabled;
    }

    static Rule create(String name, List<String> keywords, String actionType, String actionUrl, String bearerToken) {
        return new Rule(UUID.randomUUID().toString(), name, keywords, actionType, actionUrl, bearerToken, true);
    }

    boolean matches(String text) {
        if (!enabled || text == null) {
            return false;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            String needle = keyword.trim().toLowerCase(Locale.ROOT);
            if (!needle.isEmpty() && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("actionType", actionType);
        object.put("actionUrl", actionUrl);
        object.put("bearerToken", bearerToken);
        object.put("enabled", enabled);
        JSONArray words = new JSONArray();
        for (String keyword : keywords) {
            words.put(keyword);
        }
        object.put("keywords", words);
        return object;
    }

    static Rule fromJson(JSONObject object) throws JSONException {
        JSONArray words = object.optJSONArray("keywords");
        List<String> keywords = new ArrayList<>();
        if (words != null) {
            for (int i = 0; i < words.length(); i++) {
                keywords.add(words.optString(i));
            }
        }
        return new Rule(
                object.optString("id", UUID.randomUUID().toString()),
                object.optString("name", "Rule"),
                keywords,
                object.optString("actionType", ACTION_LOG),
                object.optString("actionUrl", ""),
                object.optString("bearerToken", ""),
                object.optBoolean("enabled", true)
        );
    }
}
