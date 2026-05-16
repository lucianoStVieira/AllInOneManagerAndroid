package com.allinonemanager.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class AccessManager {
    private static final String PREFS_NAME = "access_control";
    private static final String KEY_SALT = "password_salt";
    private static final String KEY_HASH = "password_hash";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int ITERATIONS = 120_000;

    private static boolean unlocked;

    private AccessManager() {
    }

    static boolean hasPassword(Context context) {
        SharedPreferences prefs = prefs(context);
        return !prefs.getString(KEY_SALT, "").isEmpty()
                && !prefs.getString(KEY_HASH, "").isEmpty();
    }

    static boolean isUnlocked() {
        return unlocked;
    }

    static void lock() {
        unlocked = false;
    }

    static void unlockWithSystemAuthentication() {
        unlocked = true;
    }

    static void createPassword(Context context, String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(password, salt);
        prefs(context)
                .edit()
                .putString(KEY_SALT, encode(salt))
                .putString(KEY_HASH, encode(hash))
                .apply();
        unlocked = true;
    }

    static boolean unlock(Context context, String password) {
        SharedPreferences prefs = prefs(context);
        String savedSalt = prefs.getString(KEY_SALT, "");
        String savedHash = prefs.getString(KEY_HASH, "");
        if (savedSalt.isEmpty() || savedHash.isEmpty()) {
            return false;
        }

        byte[] salt = decode(savedSalt);
        byte[] expectedHash = decode(savedHash);
        byte[] enteredHash = hash(password, salt);
        unlocked = MessageDigest.isEqual(expectedHash, enteredHash);
        return unlocked;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static byte[] hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to secure access password.", ex);
        }
    }

    private static String encode(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private static byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }
}
