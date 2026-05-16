package com.allinonemanager.android;

import java.time.Instant;

public final class ClientSession {
    public final long id;
    public final long clientId;
    public final String sessionAt;
    public final int durationMinutes;
    public final String sessionNotes;
    public final String createdAt;

    public ClientSession(
            long id,
            long clientId,
            String sessionAt,
            int durationMinutes,
            String sessionNotes,
            String createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.sessionAt = sessionAt;
        this.durationMinutes = durationMinutes;
        this.sessionNotes = sessionNotes;
        this.createdAt = createdAt;
    }

    public Instant sessionInstant() {
        return Instant.parse(sessionAt);
    }
}
