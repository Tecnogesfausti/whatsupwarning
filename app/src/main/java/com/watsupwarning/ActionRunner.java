package com.watsupwarning;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ActionRunner {
    private ActionRunner() {
    }

    static void run(Context context, Rule rule, String packageName, String title, String text) {
        RuleStore.recordEvent(context, "Matched \"" + rule.name + "\" from " + packageName);
        if (Rule.ACTION_HOME_ASSISTANT.equals(rule.actionType)) {
            Thread worker = new Thread(() -> callHomeAssistant(context, rule, packageName, title, text), "ha-action");
            worker.start();
        }
    }

    private static void callHomeAssistant(Context context, Rule rule, String packageName, String title, String text) {
        if (rule.actionUrl == null || rule.actionUrl.trim().isEmpty()) {
            RuleStore.recordEvent(context, "Home Assistant skipped: missing URL for " + rule.name);
            return;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(rule.actionUrl.trim());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            if (rule.bearerToken != null && !rule.bearerToken.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + rule.bearerToken.trim());
            }
            connection.setDoOutput(true);
            byte[] body = buildBody(rule, packageName, title, text).getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            int code = connection.getResponseCode();
            RuleStore.recordEvent(context, "Home Assistant response " + code + " for " + rule.name);
        } catch (Exception error) {
            RuleStore.recordEvent(context, "Home Assistant failed for " + rule.name + ": " + error.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String buildBody(Rule rule, String packageName, String title, String text) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("rule", rule.name);
        body.put("package", packageName);
        body.put("title", title);
        body.put("text", text);
        return body.toString();
    }
}
