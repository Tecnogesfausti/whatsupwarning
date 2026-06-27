# watsupwarning

Android APK project that listens to system notification popups, matches flexible keyword rules, and runs configured actions.

The first action types are:

- `Log only`: record the event inside the app.
- `Home Assistant`: call a Home Assistant webhook or API URL when matching words appear in any notification.

The requester/contact does not matter. Rules match notification title and body text only.

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
