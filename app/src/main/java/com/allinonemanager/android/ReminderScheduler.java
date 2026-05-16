package com.allinonemanager.android;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.Instant;
import java.util.List;

public final class ReminderScheduler {
    private ReminderScheduler() {
    }

    public static void scheduleAll(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        List<ReminderInfo> sessions = db.getFutureReminderCandidates();
        for (ReminderInfo info : sessions) {
            scheduleSession(context, info);
        }
    }

    public static void scheduleSession(Context context, long sessionId) {
        DatabaseHelper db = new DatabaseHelper(context);
        ReminderInfo info = db.getReminderInfo(sessionId);
        if (info != null) {
            scheduleSession(context, info);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleSession(Context context, ReminderInfo info) {
        NotificationSettings settings = NotificationSettings.from(context);
        if (!settings.sendWhatsAppAlerts() || settings.psychologistWhatsAppDigits().isEmpty()) {
            return;
        }

        long triggerAtMillis;
        try {
            Instant trigger = info.sessionInstant().plus(settings.notificationOffsetFromSessionStart());
            triggerAtMillis = trigger.toEpochMilli();
        } catch (RuntimeException ignored) {
            return;
        }

        long now = System.currentTimeMillis();
        long graceMillis = settings.graceSeconds() * 1000L;
        if (triggerAtMillis < now - graceMillis) {
            return;
        }

        if (triggerAtMillis < now) {
            triggerAtMillis = now + 2000L;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(info.sessionId),
                reminderIntent(context, info.sessionId),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancelSession(Context context, long sessionId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(sessionId),
                reminderIntent(context, sessionId),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static Intent reminderIntent(Context context, long sessionId) {
        Intent intent = new Intent(context, SessionReminderReceiver.class);
        intent.putExtra(SessionReminderReceiver.EXTRA_SESSION_ID, sessionId);
        return intent;
    }

    private static int requestCode(long sessionId) {
        return (int) (sessionId & 0x7fffffff);
    }
}
