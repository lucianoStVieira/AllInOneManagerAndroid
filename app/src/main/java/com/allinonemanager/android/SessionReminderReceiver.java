package com.allinonemanager.android;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SessionReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_SESSION_ID = "session_id";
    private static final String CHANNEL_ID = "session_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                handleReminder(context.getApplicationContext(), intent);
            } finally {
                pendingResult.finish();
                executor.shutdown();
            }
        });
    }

    private static void handleReminder(Context context, Intent intent) {
        long sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L);
        if (sessionId <= 0) {
            return;
        }

        DatabaseHelper db = new DatabaseHelper(context);
        ReminderInfo info = db.getReminderInfo(sessionId);
        if (info == null) {
            return;
        }

        Context localizedContext = AppLanguage.apply(context);
        NotificationSettings settings = NotificationSettings.from(context);
        if (!settings.sendWhatsAppAlerts()) {
            return;
        }

        String psychologistPhone = settings.psychologistWhatsAppDigits();
        if (!psychologistPhone.isEmpty()
                && !db.wasNotificationSent(sessionId, "whatsapp-psychologist")) {
            boolean posted = postWhatsAppNotification(
                    localizedContext,
                    notificationId(sessionId, 1),
                    localizedContext.getString(R.string.notification_session_alert_title_format, info.clientName),
                    localizedContext.getString(R.string.notification_psychologist_content),
                    psychologistPhone,
                    buildPsychologistMessage(localizedContext, info, settings));
            if (posted) {
                db.markNotificationSent(sessionId, "whatsapp-psychologist");
            }
        }

        if (!settings.sendClientWhatsApp()) {
            return;
        }

        String clientPhone = settings.normalizeClientPhoneForWhatsApp(info.clientPhone);
        if (clientPhone.length() >= 10
                && !clientPhone.equals(psychologistPhone)
                && !db.wasNotificationSent(sessionId, "whatsapp-client")) {
            boolean posted = postWhatsAppNotification(
                    localizedContext,
                    notificationId(sessionId, 2),
                    localizedContext.getString(R.string.notification_client_reminder_title_format, info.clientName),
                    localizedContext.getString(R.string.notification_client_content),
                    clientPhone,
                    buildClientMessage(localizedContext, info, settings));
            if (posted) {
                db.markNotificationSent(sessionId, "whatsapp-client");
            }
        }
    }

    private static boolean postWhatsAppNotification(
            Context context,
            int notificationId,
            String title,
            String content,
            String phoneDigits,
            String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return false;
        }

        ensureChannel(context, manager);

        Uri uri = Uri.parse("https://wa.me/" + phoneDigits + "?text=" + Uri.encode(message));
        Intent openWhatsApp = new Intent(Intent.ACTION_VIEW, uri);
        openWhatsApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openWhatsApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);

        manager.notify(notificationId, builder.build());
        return true;
    }

    private static void ensureChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    private static String buildPsychologistMessage(
            Context context,
            ReminderInfo info,
            NotificationSettings settings) {
        ZonedDateTime localStart = info.sessionInstant().atZone(settings.displayZone());
        Duration offset = settings.notificationOffsetFromSessionStart();
        String timing = offset.isNegative()
                ? context.getString(R.string.notification_before_session_format, formatOffset(context, offset))
                : context.getString(R.string.notification_after_session_format, formatOffset(context, offset));

        List<String> parts = new ArrayList<>();
        parts.add(context.getString(R.string.notification_psychologist_header));
        parts.add(context.getString(R.string.notification_client_format, info.clientName));
        parts.add(context.getString(
                R.string.notification_start_format,
                dateTimeFormat(context).format(localStart)));
        parts.add(context.getString(R.string.notification_duration_format, info.durationMinutes));
        parts.add(context.getString(R.string.notification_timing_format, timing));
        if (info.sessionNotes != null && !info.sessionNotes.trim().isEmpty()) {
            String note = info.sessionNotes.trim();
            parts.add(context.getString(
                    R.string.notification_notes_format,
                    note.length() > 200 ? note.substring(0, 200) + "..." : note));
        }
        return joinLines(parts);
    }

    private static String buildClientMessage(
            Context context,
            ReminderInfo info,
            NotificationSettings settings) {
        ZonedDateTime localStart = info.sessionInstant().atZone(settings.displayZone());
        Duration offset = settings.notificationOffsetFromSessionStart();
        String whenLine = offset.isNegative()
                ? context.getString(R.string.notification_client_reminder_before_format, formatOffset(context, offset))
                : context.getString(R.string.notification_client_reminder_after_format, formatOffset(context, offset));

        List<String> parts = new ArrayList<>();
        parts.add(context.getString(
                R.string.notification_client_greeting_format,
                firstName(context, info.clientName)));
        parts.add("");
        parts.add(whenLine);
        parts.add("");
        parts.add(context.getString(
                R.string.notification_client_date_time_format,
                clientDateTimeFormat(context).format(localStart)));
        parts.add(context.getString(R.string.notification_client_duration_format, info.durationMinutes));
        parts.add("");
        parts.add(context.getString(R.string.notification_client_reschedule));
        return joinLines(parts);
    }

    private static String firstName(Context context, String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return context.getString(R.string.notification_first_name_fallback);
        }

        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private static String formatOffset(Context context, Duration offset) {
        Duration value = offset.isNegative() ? offset.negated() : offset;
        long hours = value.toHours();
        if (hours >= 1 && value.minusHours(hours).isZero()) {
            return context.getResources().getQuantityString(
                    R.plurals.duration_hours,
                    (int) hours,
                    hours);
        }

        long minutes = value.toMinutes();
        if (minutes >= 1 && value.minusMinutes(minutes).isZero()) {
            return context.getResources().getQuantityString(
                    R.plurals.duration_minutes,
                    (int) minutes,
                    minutes);
        }

        long seconds = value.getSeconds();
        return context.getResources().getQuantityString(
                R.plurals.duration_seconds,
                (int) seconds,
                seconds);
    }

    private static DateTimeFormatter dateTimeFormat(Context context) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", AppLanguage.currentLocale(context));
    }

    private static DateTimeFormatter clientDateTimeFormat(Context context) {
        return DateTimeFormatter.ofPattern(
                context.getString(R.string.notification_client_date_time_pattern),
                AppLanguage.currentLocale(context));
    }

    private static String joinLines(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private static int notificationId(long sessionId, int suffix) {
        return (int) ((sessionId * 10L + suffix) & 0x7fffffff);
    }
}
