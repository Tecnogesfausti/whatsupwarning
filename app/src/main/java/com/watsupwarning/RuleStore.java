package com.watsupwarning;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class RuleStore {
    private static final String PREFS = "watsupwarning_rules";
    private static final String RULES = "rules";
    private static final String EVENTS = "events";
    private static final int MAX_EVENTS = 30;

    private RuleStore() {
    }

    static List<Rule> loadRules(Context context) {
        String raw = prefs(context).getString(RULES, "[]");
        List<Rule> rules = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                rules.add(Rule.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return rules;
    }

    static void saveRules(Context context, List<Rule> rules) {
        JSONArray array = new JSONArray();
        for (Rule rule : rules) {
            try {
                array.put(rule.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs(context).edit().putString(RULES, array.toString()).apply();
    }

    static void addRule(Context context, Rule rule) {
        List<Rule> rules = loadRules(context);
        rules.add(0, rule);
        saveRules(context, rules);
    }

    static void deleteRule(Context context, String id) {
        List<Rule> next = new ArrayList<>();
        for (Rule rule : loadRules(context)) {
            if (!rule.id.equals(id)) {
                next.add(rule);
            }
        }
        saveRules(context, next);
    }

    static void recordEvent(Context context, String message) {
        List<String> events = loadEvents(context);
        String stamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date());
        events.add(0, stamp + "  " + message);
        while (events.size() > MAX_EVENTS) {
            events.remove(events.size() - 1);
        }
        prefs(context).edit().putString(EVENTS, new JSONArray(events).toString()).apply();
    }

    static List<String> loadEvents(Context context) {
        String raw = prefs(context).getString(EVENTS, "[]");
        List<String> events = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                events.add(array.optString(i));
            }
        } catch (JSONException ignored) {
        }
        return events;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
