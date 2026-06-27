package com.watsupwarning;

import java.util.Locale;

final class NotificationEvent {
    final String packageName;
    final String title;
    final String text;

    NotificationEvent(String packageName, String title, String text) {
        this.packageName = packageName == null ? "" : packageName;
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
    }

    String searchableText() {
        return (title + "\n" + text).toLowerCase(Locale.ROOT);
    }
}
