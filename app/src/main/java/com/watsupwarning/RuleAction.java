package com.watsupwarning;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

abstract class RuleAction {
    static final String TYPE_LOG = "log";
    static final String TYPE_HOME_ASSISTANT = "home_assistant";

    final String type;

    RuleAction(String type) {
        this.type = type;
    }

    abstract void execute(Context context, Rule rule, NotificationEvent event);

    abstract String summary();

    abstract JSONObject toJson() throws JSONException;

    static RuleAction fromJson(JSONObject object) {
        String type = object.optString("type", TYPE_LOG);
        if (TYPE_HOME_ASSISTANT.equals(type)) {
            return new HomeAssistantAction(object.optString("url", ""), object.optString("bearerToken", ""));
        }
        return new LogAction();
    }
}

final class LogAction extends RuleAction {
    LogAction() {
        super(TYPE_LOG);
    }

    @Override
    void execute(Context context, Rule rule, NotificationEvent event) {
        RuleStore.recordEvent(context, "Logged \"" + rule.name + "\" from " + event.packageName);
    }

    @Override
    String summary() {
        return "Log only";
    }

    @Override
    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        return object;
    }
}

final class HomeAssistantAction extends RuleAction {
    final String url;
    final String bearerToken;

    HomeAssistantAction(String url, String bearerToken) {
        super(TYPE_HOME_ASSISTANT);
        this.url = url == null ? "" : url;
        this.bearerToken = bearerToken == null ? "" : bearerToken;
    }

    @Override
    void execute(Context context, Rule rule, NotificationEvent event) {
        Thread worker = new Thread(() -> callHomeAssistant(context, rule, event), "ha-action");
        worker.start();
    }

    @Override
    String summary() {
        return "Home Assistant";
    }

    @Override
    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("url", url);
        object.put("bearerToken", bearerToken);
        return object;
    }

    private void callHomeAssistant(Context context, Rule rule, NotificationEvent event) {
        if (url.trim().isEmpty()) {
            RuleStore.recordEvent(context, "Home Assistant skipped: missing URL for " + rule.name);
            return;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url.trim()).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            if (!bearerToken.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
            }
            connection.setDoOutput(true);
            byte[] body = buildBody(rule, event).getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            RuleStore.recordEvent(context, "Home Assistant response " + connection.getResponseCode() + " for " + rule.name);
        } catch (Exception error) {
            RuleStore.recordEvent(context, "Home Assistant failed for " + rule.name + ": " + error.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildBody(Rule rule, NotificationEvent event) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("rule", rule.name);
        body.put("package", event.packageName);
        body.put("title", event.title);
        body.put("text", event.text);
        return body.toString();
    }
}
