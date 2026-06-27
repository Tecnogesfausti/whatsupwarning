package com.watsupwarning;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.List;

public class NotificationWatchService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null || sbn.getNotification().extras == null) {
            return;
        }
        CharSequence titleValue = sbn.getNotification().extras.getCharSequence("android.title");
        CharSequence textValue = sbn.getNotification().extras.getCharSequence("android.text");
        CharSequence bigTextValue = sbn.getNotification().extras.getCharSequence("android.bigText");
        String title = titleValue == null ? "" : titleValue.toString();
        String text = firstNonEmpty(bigTextValue, textValue);
        NotificationEvent event = new NotificationEvent(sbn.getPackageName(), title, text);
        List<Rule> rules = RuleStore.loadRules(this);
        for (Rule rule : rules) {
            if (rule.matches(event)) {
                ActionRunner.run(this, rule, event);
            }
        }
    }

    private static String firstNonEmpty(CharSequence first, CharSequence second) {
        if (first != null && first.length() > 0) {
            return first.toString();
        }
        return second == null ? "" : second.toString();
    }
}
