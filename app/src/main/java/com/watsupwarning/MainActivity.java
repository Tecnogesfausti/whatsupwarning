package com.watsupwarning;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(34, 42, 43);
    private static final int MUTED = Color.rgb(100, 111, 112);
    private static final int PAPER = Color.rgb(247, 242, 234);
    private static final int CARD = Color.WHITE;
    private static final int TEAL = Color.rgb(20, 108, 108);
    private static final int GOLD = Color.rgb(244, 201, 93);
    private static final int LINE = Color.rgb(226, 222, 214);

    private LinearLayout rulesList;
    private LinearLayout eventsList;
    private TextView accessState;
    private EditText nameInput;
    private EditText wordsInput;
    private EditText urlInput;
    private EditText tokenInput;
    private Spinner actionSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("watsupwarning", 32, INK, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Keyword-triggered actions from Android notification popups.", 15, MUTED, Typeface.NORMAL));

        LinearLayout statusCard = card();
        statusCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        accessState = text("", 17, INK, Typeface.BOLD);
        statusCard.addView(accessState);
        statusCard.addView(text("Enable notification access so Android sends popup events to this app.", 14, MUTED, Typeface.NORMAL));
        Button settings = button("Open notification access", TEAL, Color.WHITE);
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        statusCard.addView(settings);
        root.addView(statusCard);

        LinearLayout form = card();
        form.addView(sectionTitle("New rule"));
        nameInput = input("Rule name, for example Temperature request");
        wordsInput = input("Words, comma separated: temp, temperatura, heat");
        actionSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Arrays.asList("Log only", "Home Assistant"));
        actionSpinner.setAdapter(adapter);
        form.addView(nameInput);
        form.addView(wordsInput);
        form.addView(actionSpinner);
        urlInput = input("Home Assistant webhook/API URL");
        tokenInput = input("Bearer token, optional");
        form.addView(urlInput);
        form.addView(tokenInput);
        actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean ha = position == 1;
                urlInput.setVisibility(ha ? View.VISIBLE : View.GONE);
                tokenInput.setVisibility(ha ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        Button add = button("Add rule", GOLD, INK);
        add.setOnClickListener(v -> addRule());
        form.addView(add);
        root.addView(form);

        LinearLayout rulesCard = card();
        rulesCard.addView(sectionTitle("Rules"));
        rulesList = new LinearLayout(this);
        rulesList.setOrientation(LinearLayout.VERTICAL);
        rulesCard.addView(rulesList);
        root.addView(rulesCard);

        LinearLayout eventsCard = card();
        eventsCard.addView(sectionTitle("Recent matches"));
        eventsList = new LinearLayout(this);
        eventsList.setOrientation(LinearLayout.VERTICAL);
        eventsCard.addView(eventsList);
        root.addView(eventsCard);

        return scroll;
    }

    private void addRule() {
        String name = nameInput.getText().toString().trim();
        List<String> words = parseWords(wordsInput.getText().toString());
        String action = actionSpinner.getSelectedItemPosition() == 1 ? Rule.ACTION_HOME_ASSISTANT : Rule.ACTION_LOG;
        if (name.isEmpty()) {
            name = words.isEmpty() ? "Untitled rule" : words.get(0);
        }
        if (words.isEmpty()) {
            Toast.makeText(this, "Add at least one word to match.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Rule.ACTION_HOME_ASSISTANT.equals(action) && urlInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Home Assistant actions need a URL.", Toast.LENGTH_SHORT).show();
            return;
        }
        RuleStore.addRule(this, Rule.create(name, words, action, urlInput.getText().toString(), tokenInput.getText().toString()));
        nameInput.setText("");
        wordsInput.setText("");
        urlInput.setText("");
        tokenInput.setText("");
        hideKeyboard();
        refresh();
    }

    private void refresh() {
        boolean enabled = notificationAccessEnabled();
        accessState.setText(enabled ? "Notification access is enabled" : "Notification access is not enabled");
        accessState.setTextColor(enabled ? TEAL : Color.rgb(149, 72, 48));

        rulesList.removeAllViews();
        List<Rule> rules = RuleStore.loadRules(this);
        if (rules.isEmpty()) {
            rulesList.addView(text("No rules yet. Add words above and choose what should happen.", 14, MUTED, Typeface.NORMAL));
        } else {
            for (Rule rule : rules) {
                rulesList.addView(ruleRow(rule));
            }
        }

        eventsList.removeAllViews();
        List<String> events = RuleStore.loadEvents(this);
        if (events.isEmpty()) {
            eventsList.addView(text("No matches recorded yet.", 14, MUTED, Typeface.NORMAL));
        } else {
            for (String event : events) {
                eventsList.addView(text(event, 13, MUTED, Typeface.NORMAL));
            }
        }
    }

    private View ruleRow(Rule rule) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView name = text(rule.name, 16, INK, Typeface.BOLD);
        row.addView(name);
        row.addView(text(TextUtils.join(", ", rule.keywords), 14, MUTED, Typeface.NORMAL));
        String action = Rule.ACTION_HOME_ASSISTANT.equals(rule.actionType) ? "Home Assistant" : "Log only";
        row.addView(text(action, 13, TEAL, Typeface.BOLD));
        Button delete = button("Delete", Color.rgb(235, 235, 232), INK);
        delete.setOnClickListener(v -> {
            RuleStore.deleteRule(this, rule.id);
            refresh();
        });
        row.addView(delete);

        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        row.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        return row;
    }

    private boolean notificationAccessEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        ComponentName component = new ComponentName(this, NotificationWatchService.class);
        return flat != null && flat.toLowerCase().contains(component.flattenToString().toLowerCase());
    }

    private List<String> parseWords(String raw) {
        List<String> words = new ArrayList<>();
        for (String piece : raw.split(",")) {
            String word = piece.trim();
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD);
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), LINE);
        card.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(18), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 19, INK, Typeface.BOLD);
        view.setPadding(0, 0, 0, dp(8));
        return view;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setLineSpacing(dp(2), 1.0f);
        view.setPadding(0, dp(3), 0, dp(3));
        return view;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(3);
        input.setBackground(fieldBackground());
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, dp(8));
        input.setLayoutParams(params);
        return input;
    }

    private Button button(String label, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(backgroundColor);
        background.setCornerRadius(dp(8));
        button.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, dp(10), 0, dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable fieldBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(251, 249, 245));
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), LINE);
        return background;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void openProjectPage(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/")));
    }
}
