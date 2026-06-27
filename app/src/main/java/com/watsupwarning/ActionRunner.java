package com.watsupwarning;

import android.content.Context;

final class ActionRunner {
    private ActionRunner() {
    }

    static void run(Context context, Rule rule, NotificationEvent event) {
        RuleStore.recordEvent(context, "Matched \"" + rule.name + "\" from " + event.packageName);
        for (RuleAction action : rule.actions) {
            action.execute(context, rule, event);
        }
    }
}
