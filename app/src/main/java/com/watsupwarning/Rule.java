package com.watsupwarning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class Rule {
    final String id;
    final String name;
    final List<RuleFilter> filters;
    final List<RuleAction> actions;
    final boolean enabled;

    Rule(String id, String name, List<RuleFilter> filters, List<RuleAction> actions, boolean enabled) {
        this.id = id;
        this.name = name;
        this.filters = filters;
        this.actions = actions;
        this.enabled = enabled;
    }

    static Rule create(String name, List<RuleFilter> filters, List<RuleAction> actions) {
        return new Rule(UUID.randomUUID().toString(), name, filters, actions, true);
    }

    boolean matches(NotificationEvent event) {
        if (!enabled || event == null || filters.isEmpty()) {
            return false;
        }
        for (RuleFilter filter : filters) {
            if (!filter.matches(event)) {
                return false;
            }
        }
        return true;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("enabled", enabled);
        JSONArray filterArray = new JSONArray();
        for (RuleFilter filter : filters) {
            filterArray.put(filter.toJson());
        }
        object.put("filters", filterArray);
        JSONArray actionArray = new JSONArray();
        for (RuleAction action : actions) {
            actionArray.put(action.toJson());
        }
        object.put("actions", actionArray);
        return object;
    }

    static Rule fromJson(JSONObject object) throws JSONException {
        List<RuleFilter> filters = new ArrayList<>();
        JSONArray filterArray = object.optJSONArray("filters");
        if (filterArray != null) {
            for (int i = 0; i < filterArray.length(); i++) {
                filters.add(RuleFilter.fromJson(filterArray.getJSONObject(i)));
            }
        }
        JSONArray legacyWords = object.optJSONArray("keywords");
        if (filters.isEmpty() && legacyWords != null) {
            filters.add(new KeywordFilter(RuleFilter.readStringArray(legacyWords)));
        }

        List<RuleAction> actions = new ArrayList<>();
        JSONArray actionArray = object.optJSONArray("actions");
        if (actionArray != null) {
            for (int i = 0; i < actionArray.length(); i++) {
                actions.add(RuleAction.fromJson(actionArray.getJSONObject(i)));
            }
        }
        String legacyAction = object.optString("actionType", "");
        if (actions.isEmpty() && RuleAction.TYPE_HOME_ASSISTANT.equals(legacyAction)) {
            actions.add(new HomeAssistantAction(object.optString("actionUrl", ""), object.optString("bearerToken", "")));
        }
        if (actions.isEmpty()) {
            actions.add(new LogAction());
        }

        return new Rule(
                object.optString("id", UUID.randomUUID().toString()),
                object.optString("name", "Rule"),
                filters,
                actions,
                object.optBoolean("enabled", true)
        );
    }
}
