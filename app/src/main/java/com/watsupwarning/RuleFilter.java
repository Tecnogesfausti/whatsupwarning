package com.watsupwarning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class RuleFilter {
    static final String TYPE_KEYWORDS = "keywords";
    static final String TYPE_PACKAGE = "package";

    final String type;

    RuleFilter(String type) {
        this.type = type;
    }

    abstract boolean matches(NotificationEvent event);

    abstract String summary();

    abstract JSONObject toJson() throws JSONException;

    static RuleFilter fromJson(JSONObject object) throws JSONException {
        String type = object.optString("type", TYPE_KEYWORDS);
        if (TYPE_PACKAGE.equals(type)) {
            return new PackageFilter(object.optString("packageName", ""));
        }
        return new KeywordFilter(readStringArray(object.optJSONArray("words")));
    }

    static List<String> readStringArray(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i).trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    static JSONArray writeStringArray(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array;
    }
}

final class KeywordFilter extends RuleFilter {
    final List<String> words;

    KeywordFilter(List<String> words) {
        super(TYPE_KEYWORDS);
        this.words = words;
    }

    @Override
    boolean matches(NotificationEvent event) {
        String haystack = event.searchableText();
        for (String word : words) {
            String needle = word.trim().toLowerCase(Locale.ROOT);
            if (!needle.isEmpty() && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Override
    String summary() {
        return "Words: " + android.text.TextUtils.join(", ", words);
    }

    @Override
    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("words", writeStringArray(words));
        return object;
    }
}

final class PackageFilter extends RuleFilter {
    final String packageName;

    PackageFilter(String packageName) {
        super(TYPE_PACKAGE);
        this.packageName = packageName == null ? "" : packageName.trim();
    }

    @Override
    boolean matches(NotificationEvent event) {
        return packageName.isEmpty() || event.packageName.equals(packageName);
    }

    @Override
    String summary() {
        return packageName.isEmpty() ? "Any app" : "App: " + packageName;
    }

    @Override
    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("packageName", packageName);
        return object;
    }
}
