package com.allinonemanager.android;

public final class Client {
    public final long id;
    public final String fullName;
    public final String phone;
    public final String email;
    public final String dateOfBirth;
    public final String timeZoneId;
    public final String notes;
    public final String createdAt;

    public Client(
            long id,
            String fullName,
            String phone,
            String email,
            String dateOfBirth,
            String timeZoneId,
            String notes,
            String createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.timeZoneId = TimeZoneSupport.normalizeZoneId(timeZoneId);
        this.notes = notes;
        this.createdAt = createdAt;
    }
}
