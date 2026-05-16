package com.allinonemanager.android;

import java.time.Instant;
import java.time.ZoneId;

public final class ReminderInfo {
    public final long sessionId;
    public final long clientId;
    public final String sessionAt;
    public final int durationMinutes;
    public final String sessionNotes;
    public final String clientName;
    public final String clientPhone;
    public final String clientTimeZoneId;

    public ReminderInfo(
            long sessionId,
            long clientId,
            String sessionAt,
            int durationMinutes,
            String sessionNotes,
            String clientName,
            String clientPhone,
            String clientTimeZoneId) {
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.sessionAt = sessionAt;
        this.durationMinutes = durationMinutes;
        this.sessionNotes = sessionNotes;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.clientTimeZoneId = TimeZoneSupport.normalizeZoneId(clientTimeZoneId);
    }

    public Instant sessionInstant() {
        return Instant.parse(sessionAt);
    }

    public ZoneId clientZone() {
        return TimeZoneSupport.zoneId(clientTimeZoneId);
    }
}
