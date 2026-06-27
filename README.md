# watsupwarning

Android APK project that listens to system notification popups, matches flexible rules, and runs configured actions.

The first action types are:

- `Log only`: record the event inside the app.
- `Home Assistant`: call a Home Assistant webhook or API URL when matching words appear in any notification.

The requester/contact does not matter. Rules match notification title and body text only.

## Rule Engine

The notification listener creates a `NotificationEvent`, then each saved `Rule` evaluates:

```text
all filters match -> run all actions
```

Core extension points:

- Add new filters in `RuleFilter.java`, for example regex, exact phrase, package, time window, or sender text.
- Add new actions in `RuleAction.java`, for example Home Assistant service calls, local Android intents, SMS replies, or HTTP webhooks.
- Keep `NotificationWatchService` boring: it should only convert Android notifications into `NotificationEvent` and hand them to the rule engine.

Current filters:

- `KeywordFilter`: matches any configured word in notification title/body.
- `PackageFilter`: optionally limits the rule to one Android package such as `com.whatsapp`.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Phone Setup

1. Install the APK.
2. Open **watsupwarning**.
3. Tap the notification-access button and enable **watsupwarning**.
4. Add keyword rules in the app.

For WhatsApp-triggered automations, make sure WhatsApp notifications are visible to Android.
