package com.allinonemanager.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Duration;
import java.time.ZoneId;
import java.util.regex.Pattern;

public final class NotificationSettings {
    private static final String PREFS_NAME = "notification_settings";
    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");

    private static final String KEY_PSYCHOLOGIST_WHATSAPP = "psychologist_whatsapp_digits";
    private static final String KEY_SEND_WHATSAPP = "send_whatsapp_alerts";
    private static final String KEY_SEND_CLIENT_WHATSAPP = "send_client_whatsapp";
    private static final String KEY_CLIENT_PREFIX = "client_phone_international_prefix";
    private static final String KEY_ALERT_BEFORE = "alert_before_session";
    private static final String KEY_ALERT_MINUTES = "alert_minutes";
    private static final String KEY_GRACE_SECONDS = "notify_grace_seconds";
    private static final String KEY_DISPLAY_ZONE = "display_time_zone_id";
    private static final String KEY_SEND_SMS = "send_sms_alerts";
    private static final String KEY_PSYCHOLOGIST_SMS = "psychologist_sms_phone";
    private static final String KEY_SEND_CLIENT_SMS = "send_client_sms";
    private static final String KEY_TWILIO_ACCOUNT_SID = "twilio_account_sid";
    private static final String KEY_TWILIO_AUTH_TOKEN = "twilio_auth_token";
    private static final String KEY_TWILIO_FROM_PHONE = "twilio_from_phone_number";
    private static final String KEY_TWILIO_MESSAGING_SERVICE = "twilio_messaging_service_sid";

    private final SharedPreferences prefs;

    private NotificationSettings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static NotificationSettings from(Context context) {
        return new NotificationSettings(context);
    }

    public String psychologistWhatsAppDigits() {
        return normalizeDigits(prefs.getString(KEY_PSYCHOLOGIST_WHATSAPP, ""));
    }

    public boolean sendWhatsAppAlerts() {
        return prefs.getBoolean(KEY_SEND_WHATSAPP, true);
    }

    public boolean sendClientWhatsApp() {
        return prefs.getBoolean(KEY_SEND_CLIENT_WHATSAPP, true);
    }

    public String clientPhoneInternationalPrefixDigits() {
        return normalizeDigits(prefs.getString(KEY_CLIENT_PREFIX, ""));
    }

    public boolean alertBeforeSession() {
        return prefs.getBoolean(KEY_ALERT_BEFORE, true);
    }

    public int alertMinutes() {
        return clamp(prefs.getInt(KEY_ALERT_MINUTES, 60), 1, 10080);
    }

    public Duration notificationOffsetFromSessionStart() {
        Duration minutes = Duration.ofMinutes(alertMinutes());
        return alertBeforeSession() ? minutes.negated() : minutes;
    }

    public int graceSeconds() {
        return clamp(prefs.getInt(KEY_GRACE_SECONDS, 900), 60, 86400);
    }

    public String displayTimeZoneId() {
        return prefs.getString(KEY_DISPLAY_ZONE, "America/Sao_Paulo");
    }

    public ZoneId displayZone() {
        String id = mapWindowsTimeZone(displayTimeZoneId());
        if (id == null || id.trim().isEmpty()) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(id.trim());
        } catch (RuntimeException ignored) {
            return ZoneId.systemDefault();
        }
    }

    public boolean sendSmsAlerts() {
        return prefs.getBoolean(KEY_SEND_SMS, false);
    }

    public String psychologistSmsPhone() {
        return prefs.getString(KEY_PSYCHOLOGIST_SMS, "");
    }

    public boolean sendClientSms() {
        return prefs.getBoolean(KEY_SEND_CLIENT_SMS, true);
    }

    public String twilioAccountSid() {
        return prefs.getString(KEY_TWILIO_ACCOUNT_SID, "");
    }

    public String twilioAuthToken() {
        return prefs.getString(KEY_TWILIO_AUTH_TOKEN, "");
    }

    public String twilioFromPhoneNumber() {
        return prefs.getString(KEY_TWILIO_FROM_PHONE, "");
    }

    public String twilioMessagingServiceSid() {
        return prefs.getString(KEY_TWILIO_MESSAGING_SERVICE, "");
    }

    public String normalizeClientPhoneForWhatsApp(String rawPhone) {
        String digits = normalizeDigits(rawPhone);
        if (digits.isEmpty()) {
            return "";
        }

        String prefix = clientPhoneInternationalPrefixDigits();
        if (!prefix.isEmpty()
                && !digits.startsWith(prefix)
                && digits.length() >= 8
                && digits.length() <= 11) {
            return prefix + digits;
        }

        return digits;
    }

    public void save(
            String psychologistWhatsAppDigits,
            boolean sendWhatsAppAlerts,
            boolean sendClientWhatsApp,
            String clientPhoneInternationalPrefix,
            boolean alertBeforeSession,
            int alertMinutes,
            int graceSeconds,
            String displayTimeZoneId,
            boolean sendSmsAlerts,
            String psychologistSmsPhone,
            boolean sendClientSms,
            String twilioAccountSid,
            String twilioAuthToken,
            String twilioFromPhoneNumber,
            String twilioMessagingServiceSid) {
        prefs.edit()
                .putString(KEY_PSYCHOLOGIST_WHATSAPP, normalizeDigits(psychologistWhatsAppDigits))
                .putBoolean(KEY_SEND_WHATSAPP, sendWhatsAppAlerts)
                .putBoolean(KEY_SEND_CLIENT_WHATSAPP, sendClientWhatsApp)
                .putString(KEY_CLIENT_PREFIX, normalizeDigits(clientPhoneInternationalPrefix))
                .putBoolean(KEY_ALERT_BEFORE, alertBeforeSession)
                .putInt(KEY_ALERT_MINUTES, clamp(alertMinutes, 1, 10080))
                .putInt(KEY_GRACE_SECONDS, clamp(graceSeconds, 60, 86400))
                .putString(KEY_DISPLAY_ZONE, displayTimeZoneId == null ? "" : displayTimeZoneId.trim())
                .putBoolean(KEY_SEND_SMS, sendSmsAlerts)
                .putString(KEY_PSYCHOLOGIST_SMS, psychologistSmsPhone == null ? "" : psychologistSmsPhone.trim())
                .putBoolean(KEY_SEND_CLIENT_SMS, sendClientSms)
                .putString(KEY_TWILIO_ACCOUNT_SID, twilioAccountSid == null ? "" : twilioAccountSid.trim())
                .putString(KEY_TWILIO_AUTH_TOKEN, twilioAuthToken == null ? "" : twilioAuthToken.trim())
                .putString(KEY_TWILIO_FROM_PHONE, twilioFromPhoneNumber == null ? "" : twilioFromPhoneNumber.trim())
                .putString(KEY_TWILIO_MESSAGING_SERVICE, twilioMessagingServiceSid == null ? "" : twilioMessagingServiceSid.trim())
                .apply();
    }

    public static String normalizeDigits(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }

        return NON_DIGITS.matcher(raw).replaceAll("");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String mapWindowsTimeZone(String id) {
        if (id == null) {
            return "";
        }

        if ("E. South America Standard Time".equalsIgnoreCase(id.trim())) {
            return "America/Sao_Paulo";
        }

        return id;
    }
}
