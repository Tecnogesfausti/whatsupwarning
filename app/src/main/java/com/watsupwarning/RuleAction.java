package com.watsupwarning;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

abstract class RuleAction {
    static final String TYPE_LOG = "log";
    static final String TYPE_HOME_ASSISTANT = "home_assistant";
    static final String TYPE_SPEAK_HOME_ASSISTANT_STATE = "speak_home_assistant_state";

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
        if (TYPE_SPEAK_HOME_ASSISTANT_STATE.equals(type)) {
            return new SpeakHomeAssistantStateAction(
                    object.optString("entityId", ""),
                    object.optString("label", ""),
                    object.optString("bearerToken", "")
            );
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

final class SpeakHomeAssistantStateAction extends RuleAction {
    final String entityId;
    final String label;
    final String bearerToken;

    SpeakHomeAssistantStateAction(String entityId, String label, String bearerToken) {
        super(TYPE_SPEAK_HOME_ASSISTANT_STATE);
        this.entityId = entityId == null ? "" : entityId.trim();
        this.label = label == null ? "" : label.trim();
        this.bearerToken = bearerToken == null ? "" : bearerToken.trim();
    }

    @Override
    void execute(Context context, Rule rule, NotificationEvent event) {
        Thread worker = new Thread(() -> fetchAndSpeak(context, rule), "ha-speak-state");
        worker.start();
    }

    @Override
    String summary() {
        return "Speak HA state: " + entityId;
    }

    @Override
    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("entityId", entityId);
        object.put("label", label);
        object.put("bearerToken", bearerToken);
        return object;
    }

    private void fetchAndSpeak(Context context, Rule rule) {
        if (entityId.isEmpty()) {
            RuleStore.recordEvent(context, "Home Assistant state skipped: missing entity for " + rule.name);
            return;
        }
        String url = HomeAssistantSettings.resolveUrl(context, "/api/states/" + entityId);
        if (url.isEmpty()) {
            RuleStore.recordEvent(context, "Home Assistant state skipped: missing base URL");
            return;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            String token = bearerToken.isEmpty() ? HomeAssistantSettings.getToken(context) : bearerToken;
            if (!token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                RuleStore.recordEvent(context, "Home Assistant state failed " + code + " for " + entityId);
                SpeechAnnouncer.speak(context, "No he podido consultar la temperatura de la casa.");
                return;
            }
            JSONObject state = new JSONObject(readAll(connection.getInputStream()));
            String value = state.optString("state", "");
            String unit = state.optJSONObject("attributes") == null
                    ? ""
                    : state.optJSONObject("attributes").optString("unit_of_measurement", "");
            String spoken = buildSpokenMessage(value, unit);
            RuleStore.recordEvent(context, "Spoke Home Assistant state " + entityId + ": " + value + unit);
            SpeechAnnouncer.speak(context, spoken);
        } catch (Exception error) {
            RuleStore.recordEvent(context, "Home Assistant state failed for " + entityId + ": " + error.getMessage());
            SpeechAnnouncer.speak(context, "No he podido consultar la temperatura de la casa.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildSpokenMessage(String value, String unit) {
        String name = label.isEmpty() ? "la casa" : label;
        String cleanUnit = unit == null ? "" : unit.trim();
        if ("°C".equals(cleanUnit) || "C".equalsIgnoreCase(cleanUnit)) {
            cleanUnit = "grados";
        }
        String cleanValue = value == null ? "" : value.trim().replace(".", ",");
        if (cleanValue.isEmpty() || "unknown".equals(cleanValue.toLowerCase(Locale.ROOT))) {
            return "No tengo una lectura válida de temperatura para " + name + ".";
        }
        return "La temperatura de " + name + " es " + cleanValue + (cleanUnit.isEmpty() ? "" : " " + cleanUnit) + ".";
    }

    private String readAll(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
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
        String resolvedUrl = HomeAssistantSettings.resolveUrl(context, url);
        if (resolvedUrl.trim().isEmpty()) {
            RuleStore.recordEvent(context, "Home Assistant skipped: missing URL for " + rule.name);
            return;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(resolvedUrl).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            String token = bearerToken.trim().isEmpty() ? HomeAssistantSettings.getToken(context) : bearerToken.trim();
            if (!token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
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
