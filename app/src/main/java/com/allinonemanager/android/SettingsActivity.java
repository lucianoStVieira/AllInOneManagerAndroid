package com.allinonemanager.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends Activity {
    private static final String[] LANGUAGE_TAGS = {
            AppLanguage.SYSTEM_DEFAULT,
            "en",
            "pt"
    };

    private NotificationSettings settings;

    private Spinner spinnerLanguage;
    private EditText txtPsychologistWhatsApp;
    private Switch swSendWhatsApp;
    private Switch swSendClientWhatsApp;
    private EditText txtClientPrefix;
    private Spinner spinnerTiming;
    private EditText txtAlertMinutes;
    private EditText txtGraceSeconds;
    private Switch swSendSms;
    private EditText txtPsychologistSms;
    private Switch swSendClientSms;
    private EditText txtTwilioAccountSid;
    private EditText txtTwilioAuthToken;
    private EditText txtTwilioFromPhone;
    private EditText txtTwilioMessagingService;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLanguage.apply(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiKit.applyWindow(this);
        settings = NotificationSettings.from(this);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(UiKit.COLOR_BACKGROUND);
        LinearLayout root = vertical();
        root.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(14), dp(16));
        header.setBackground(UiKit.headerBackground(this));
        TextView title = new TextView(this);
        title.setText(R.string.settings_title);
        UiKit.styleHeaderTitle(title);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button close = UiKit.neutralButton(this, R.string.action_cancel, R.drawable.ic_clear_24);
        close.setOnClickListener(v -> finish());
        header.addView(close);
        LinearLayout.LayoutParams headerParams = fullWidthParams();
        headerParams.setMargins(0, 0, 0, dp(8));
        root.addView(header, headerParams);

        addSectionTitle(root, R.string.section_language, R.drawable.ic_language_24);
        spinnerLanguage = new Spinner(this);
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] {
                        getString(R.string.language_system_default),
                        getString(R.string.language_english),
                        getString(R.string.language_portuguese)
                });
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(languageAdapter);
        spinnerLanguage.setSelection(AppLanguage.selectedLanguageIndex(this, LANGUAGE_TAGS));
        UiKit.styleSpinner(spinnerLanguage);
        root.addView(spinnerLanguage);

        addSectionTitle(root, R.string.section_whatsapp, R.drawable.ic_chat_24);
        swSendWhatsApp = new Switch(this);
        swSendWhatsApp.setText(R.string.setting_send_whatsapp);
        swSendWhatsApp.setChecked(settings.sendWhatsAppAlerts());
        UiKit.styleSwitch(swSendWhatsApp);
        root.addView(swSendWhatsApp);

        txtPsychologistWhatsApp = input(
                R.string.hint_psychologist_whatsapp,
                InputType.TYPE_CLASS_PHONE,
                R.drawable.ic_phone_24);
        txtPsychologistWhatsApp.setText(settings.psychologistWhatsAppDigits());
        root.addView(txtPsychologistWhatsApp);

        swSendClientWhatsApp = new Switch(this);
        swSendClientWhatsApp.setText(R.string.setting_send_client_whatsapp);
        swSendClientWhatsApp.setChecked(settings.sendClientWhatsApp());
        UiKit.styleSwitch(swSendClientWhatsApp);
        root.addView(swSendClientWhatsApp);

        txtClientPrefix = input(
                R.string.hint_default_country_prefix,
                InputType.TYPE_CLASS_PHONE,
                R.drawable.ic_phone_24);
        txtClientPrefix.setText(settings.clientPhoneInternationalPrefixDigits());
        root.addView(txtClientPrefix);

        addSectionTitle(root, R.string.section_timing, R.drawable.ic_alarm_24);
        spinnerTiming = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] {
                        getString(R.string.timing_before_session),
                        getString(R.string.timing_after_session)
                });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTiming.setAdapter(adapter);
        spinnerTiming.setSelection(settings.alertBeforeSession() ? 0 : 1);
        UiKit.styleSpinner(spinnerTiming);
        root.addView(spinnerTiming);

        txtAlertMinutes = input(R.string.hint_alert_minutes, InputType.TYPE_CLASS_NUMBER, R.drawable.ic_timer_24);
        txtAlertMinutes.setText(String.valueOf(settings.alertMinutes()));
        txtGraceSeconds = input(R.string.hint_grace_seconds, InputType.TYPE_CLASS_NUMBER, R.drawable.ic_clock_24);
        txtGraceSeconds.setText(String.valueOf(settings.graceSeconds()));
        root.addView(twoColumn(txtAlertMinutes, txtGraceSeconds));

        addSectionTitle(root, R.string.section_twilio_sms, R.drawable.ic_sms_24);
        swSendSms = new Switch(this);
        swSendSms.setText(R.string.setting_save_sms);
        swSendSms.setChecked(settings.sendSmsAlerts());
        UiKit.styleSwitch(swSendSms);
        root.addView(swSendSms);

        txtPsychologistSms = input(R.string.hint_psychologist_sms, InputType.TYPE_CLASS_PHONE, R.drawable.ic_phone_24);
        txtPsychologistSms.setText(settings.psychologistSmsPhone());
        root.addView(txtPsychologistSms);

        swSendClientSms = new Switch(this);
        swSendClientSms.setText(R.string.setting_send_client_sms);
        swSendClientSms.setChecked(settings.sendClientSms());
        UiKit.styleSwitch(swSendClientSms);
        root.addView(swSendClientSms);

        txtTwilioAccountSid = input(R.string.hint_twilio_account_sid, InputType.TYPE_CLASS_TEXT, R.drawable.ic_key_24);
        txtTwilioAccountSid.setText(settings.twilioAccountSid());
        txtTwilioAuthToken = input(
                R.string.hint_twilio_auth_token,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                R.drawable.ic_key_24);
        txtTwilioAuthToken.setText(settings.twilioAuthToken());
        txtTwilioFromPhone = input(R.string.hint_twilio_from_phone, InputType.TYPE_CLASS_PHONE, R.drawable.ic_phone_24);
        txtTwilioFromPhone.setText(settings.twilioFromPhoneNumber());
        txtTwilioMessagingService = input(
                R.string.hint_twilio_messaging_service,
                InputType.TYPE_CLASS_TEXT,
                R.drawable.ic_sms_24);
        txtTwilioMessagingService.setText(settings.twilioMessagingServiceSid());
        root.addView(txtTwilioAccountSid);
        root.addView(txtTwilioAuthToken);
        root.addView(txtTwilioFromPhone);
        root.addView(txtTwilioMessagingService);

        LinearLayout buttons = horizontal();
        buttons.setGravity(Gravity.CENTER);
        Button save = UiKit.compactPrimaryButton(this, R.string.action_save, R.drawable.ic_save_24);
        save.setOnClickListener(v -> saveSettings());
        Button cancel = UiKit.compactNeutralButton(this, R.string.action_cancel, R.drawable.ic_clear_24);
        cancel.setOnClickListener(v -> finish());
        addCompact(buttons, save);
        addCompact(buttons, cancel);
        root.addView(buttons);
    }

    private void saveSettings() {
        int minutes = parseInt(txtAlertMinutes, 60);
        int grace = parseInt(txtGraceSeconds, 900);
        AppLanguage.saveSelectedLanguageTag(this, selectedLanguageTag());
        settings.save(
                txtPsychologistWhatsApp.getText().toString(),
                swSendWhatsApp.isChecked(),
                swSendClientWhatsApp.isChecked(),
                txtClientPrefix.getText().toString(),
                spinnerTiming.getSelectedItemPosition() == 0,
                minutes,
                grace,
                swSendSms.isChecked(),
                txtPsychologistSms.getText().toString(),
                swSendClientSms.isChecked(),
                txtTwilioAccountSid.getText().toString(),
                txtTwilioAuthToken.getText().toString(),
                txtTwilioFromPhone.getText().toString(),
                txtTwilioMessagingService.getText().toString());
        ReminderScheduler.scheduleAll(this);
        Context toastContext = AppLanguage.apply(this);
        Toast.makeText(
                toastContext,
                toastContext.getString(R.string.toast_settings_saved),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private String selectedLanguageTag() {
        int selected = spinnerLanguage.getSelectedItemPosition();
        if (selected < 0 || selected >= LANGUAGE_TAGS.length) {
            return AppLanguage.SYSTEM_DEFAULT;
        }

        return LANGUAGE_TAGS[selected];
    }

    private int parseInt(EditText editText, int fallback) {
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private EditText input(int hintResId, int inputType, int iconResId) {
        EditText editText = new EditText(this);
        editText.setHint(hintResId);
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        UiKit.styleInput(editText, iconResId);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(6));
        editText.setLayoutParams(params);
        return editText;
    }

    private LinearLayout twoColumn(View left, View right) {
        LinearLayout row = horizontal();
        addWeighted(row, left);
        addWeighted(row, right);
        return row;
    }

    private void addWeighted(LinearLayout row, View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(dp(3), dp(6), dp(3), dp(6));
        row.addView(view, params);
    }

    private void addCompact(LinearLayout row, View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMargins(dp(3), dp(6), dp(3), dp(6));
        row.addView(view, params);
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void addSectionTitle(LinearLayout root, int textResId, int iconResId) {
        TextView title = new TextView(this);
        title.setText(textResId);
        UiKit.styleSectionTitle(title, iconResId);
        root.addView(title);
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private int dp(int value) {
        return UiKit.dp(this, value);
    }
}
