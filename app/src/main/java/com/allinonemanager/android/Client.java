package com.allinonemanager.android;

public final class Client {
    public final long id;
    public final String fullName;
    public final String phone;
    public final String email;
    public final String dateOfBirth;
    public final String notes;
    public final String createdAt;

    public Client(
            long id,
            String fullName,
            String phone,
            String email,
            String dateOfBirth,
            String notes,
            String createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.notes = notes;
        this.createdAt = createdAt;
    }
}
