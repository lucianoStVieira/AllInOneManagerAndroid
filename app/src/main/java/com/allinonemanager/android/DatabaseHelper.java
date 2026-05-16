package com.allinonemanager.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "psychologist_clients.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS Clients ("
                        + "Id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "FullName TEXT NOT NULL,"
                        + "Phone TEXT,"
                        + "Email TEXT,"
                        + "DateOfBirth TEXT,"
                        + "TimeZoneId TEXT NOT NULL DEFAULT '" + TimeZoneSupport.DEFAULT_ZONE_ID + "',"
                        + "Notes TEXT,"
                        + "CreatedAt TEXT NOT NULL"
                        + ");");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS Sessions ("
                        + "Id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "ClientId INTEGER NOT NULL,"
                        + "SessionAt TEXT NOT NULL,"
                        + "DurationMinutes INTEGER NOT NULL,"
                        + "SessionNotes TEXT,"
                        + "CreatedAt TEXT NOT NULL,"
                        + "FOREIGN KEY (ClientId) REFERENCES Clients(Id) ON DELETE CASCADE"
                        + ");");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS NotificationLog ("
                        + "SessionId INTEGER NOT NULL,"
                        + "Channel TEXT NOT NULL,"
                        + "CreatedAt TEXT NOT NULL,"
                        + "PRIMARY KEY (SessionId, Channel)"
                        + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            ensureClientTimeZoneColumn(db);
        }
        onCreate(db);
    }

    public long addClient(
            String fullName,
            String phone,
            String email,
            String dateOfBirth,
            String timeZoneId,
            String notes) {
        ContentValues values = clientValues(fullName, phone, email, dateOfBirth, timeZoneId, notes);
        values.put("CreatedAt", Instant.now().toString());
        return getWritableDatabase().insertOrThrow("Clients", null, values);
    }

    public boolean updateClient(
            long id,
            String fullName,
            String phone,
            String email,
            String dateOfBirth,
            String timeZoneId,
            String notes) {
        ContentValues values = clientValues(fullName, phone, email, dateOfBirth, timeZoneId, notes);
        int updated = getWritableDatabase().update("Clients", values, "Id = ?", new String[] { String.valueOf(id) });
        return updated > 0;
    }

    public boolean deleteClient(long id) {
        int deleted = getWritableDatabase().delete("Clients", "Id = ?", new String[] { String.valueOf(id) });
        return deleted > 0;
    }

    public List<Client> getClients(String searchTerm) {
        StringBuilder sql = new StringBuilder(
                "SELECT Id, FullName, Phone, Email, DateOfBirth, TimeZoneId, Notes, CreatedAt FROM Clients");
        String[] args = null;

        String search = safe(searchTerm).trim();
        if (!search.isEmpty()) {
            sql.append(" WHERE FullName LIKE ? OR Phone LIKE ? OR Email LIKE ?");
            String like = "%" + search + "%";
            args = new String[] { like, like, like };
        }

        sql.append(" ORDER BY datetime(CreatedAt) DESC;");

        List<Client> clients = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(sql.toString(), args)) {
            while (cursor.moveToNext()) {
                clients.add(readClient(cursor));
            }
        }

        return clients;
    }

    public Client getClient(long id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT Id, FullName, Phone, Email, DateOfBirth, TimeZoneId, Notes, CreatedAt "
                        + "FROM Clients WHERE Id = ?;",
                new String[] { String.valueOf(id) })) {
            return cursor.moveToFirst() ? readClient(cursor) : null;
        }
    }

    public long addSession(long clientId, String sessionAt, int durationMinutes, String sessionNotes) {
        ContentValues values = new ContentValues();
        values.put("ClientId", clientId);
        values.put("SessionAt", sessionAt);
        values.put("DurationMinutes", durationMinutes);
        values.put("SessionNotes", safe(sessionNotes).trim());
        values.put("CreatedAt", Instant.now().toString());
        return getWritableDatabase().insertOrThrow("Sessions", null, values);
    }

    public boolean deleteSession(long sessionId, long clientId) {
        int deleted = getWritableDatabase().delete(
                "Sessions",
                "Id = ? AND ClientId = ?",
                new String[] { String.valueOf(sessionId), String.valueOf(clientId) });
        return deleted > 0;
    }

    public List<ClientSession> getSessionsForClient(long clientId) {
        List<ClientSession> sessions = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT Id, ClientId, SessionAt, DurationMinutes, SessionNotes, CreatedAt "
                        + "FROM Sessions WHERE ClientId = ? ORDER BY datetime(SessionAt) DESC;",
                new String[] { String.valueOf(clientId) })) {
            while (cursor.moveToNext()) {
                sessions.add(readSession(cursor));
            }
        }

        return sessions;
    }

    public List<ReminderInfo> getFutureReminderCandidates() {
        String from = Instant.now().minusSeconds(24L * 60L * 60L).toString();
        List<ReminderInfo> sessions = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT s.Id, s.ClientId, s.SessionAt, s.DurationMinutes, s.SessionNotes, "
                        + "c.FullName, c.Phone, c.TimeZoneId "
                        + "FROM Sessions s INNER JOIN Clients c ON c.Id = s.ClientId "
                        + "WHERE s.SessionAt >= ? ORDER BY datetime(s.SessionAt);",
                new String[] { from })) {
            while (cursor.moveToNext()) {
                sessions.add(readReminderInfo(cursor));
            }
        }

        return sessions;
    }

    public ReminderInfo getReminderInfo(long sessionId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT s.Id, s.ClientId, s.SessionAt, s.DurationMinutes, s.SessionNotes, "
                        + "c.FullName, c.Phone, c.TimeZoneId "
                        + "FROM Sessions s INNER JOIN Clients c ON c.Id = s.ClientId WHERE s.Id = ?;",
                new String[] { String.valueOf(sessionId) })) {
            return cursor.moveToFirst() ? readReminderInfo(cursor) : null;
        }
    }

    public boolean wasNotificationSent(long sessionId, String channel) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM NotificationLog WHERE SessionId = ? AND Channel = ? LIMIT 1;",
                new String[] { String.valueOf(sessionId), channel })) {
            return cursor.moveToFirst();
        }
    }

    public void markNotificationSent(long sessionId, String channel) {
        ContentValues values = new ContentValues();
        values.put("SessionId", sessionId);
        values.put("Channel", channel);
        values.put("CreatedAt", Instant.now().toString());
        getWritableDatabase().insertWithOnConflict(
                "NotificationLog",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static ContentValues clientValues(
            String fullName,
            String phone,
            String email,
            String dateOfBirth,
            String timeZoneId,
            String notes) {
        ContentValues values = new ContentValues();
        values.put("FullName", safe(fullName).trim());
        values.put("Phone", safe(phone).trim());
        values.put("Email", safe(email).trim());
        values.put("DateOfBirth", safe(dateOfBirth).trim());
        values.put("TimeZoneId", TimeZoneSupport.normalizeZoneId(timeZoneId));
        values.put("Notes", safe(notes).trim());
        return values;
    }

    private static Client readClient(Cursor cursor) {
        return new Client(
                cursor.getLong(cursor.getColumnIndexOrThrow("Id")),
                getString(cursor, "FullName"),
                getString(cursor, "Phone"),
                getString(cursor, "Email"),
                getString(cursor, "DateOfBirth"),
                getString(cursor, "TimeZoneId"),
                getString(cursor, "Notes"),
                getString(cursor, "CreatedAt"));
    }

    private static ClientSession readSession(Cursor cursor) {
        return new ClientSession(
                cursor.getLong(cursor.getColumnIndexOrThrow("Id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("ClientId")),
                getString(cursor, "SessionAt"),
                cursor.getInt(cursor.getColumnIndexOrThrow("DurationMinutes")),
                getString(cursor, "SessionNotes"),
                getString(cursor, "CreatedAt"));
    }

    private static ReminderInfo readReminderInfo(Cursor cursor) {
        return new ReminderInfo(
                cursor.getLong(0),
                cursor.getLong(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.isNull(4) ? "" : cursor.getString(4),
                cursor.isNull(5) ? "Client" : cursor.getString(5),
                cursor.isNull(6) ? "" : cursor.getString(6),
                cursor.isNull(7) ? TimeZoneSupport.DEFAULT_ZONE_ID : cursor.getString(7));
    }

    private static void ensureClientTimeZoneColumn(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(Clients);", null)) {
            while (cursor.moveToNext()) {
                if ("TimeZoneId".equalsIgnoreCase(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                    return;
                }
            }
        }

        db.execSQL(
                "ALTER TABLE Clients ADD COLUMN TimeZoneId TEXT NOT NULL DEFAULT '"
                        + TimeZoneSupport.DEFAULT_ZONE_ID
                        + "';");
    }

    private static String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
