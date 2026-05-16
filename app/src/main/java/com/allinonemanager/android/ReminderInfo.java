package com.allinonemanager.android;

import java.time.Instant;

public final class ReminderInfo {
    public final long sessionId;
    public final long clientId;
    public final String sessionAt;
    public final int durationMinutes;
    public final String sessionNotes;
    public final String clientName;
    public final String clientPhone;

    public ReminderInfo(
            long sessionId,
            long clientId,
            String sessionAt,
            int durationMinutes,
            String sessionNotes,
            String clientName,
            String clientPhone) {
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.sessionAt = sessionAt;
        this.durationMinutes = durationMinutes;
        this.sessionNotes = sessionNotes;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
    }

    public Instant sessionInstant() {
        return Instant.parse(sessionAt);
    }
}
