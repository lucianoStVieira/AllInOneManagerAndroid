package com.allinonemanager.android;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TimeZoneSupport {
    static final String DEFAULT_ZONE_ID = "America/Sao_Paulo";

    private static final List<String> AVAILABLE_ZONE_IDS = buildAvailableZoneIds();

    private TimeZoneSupport() {
    }

    static List<String> availableZoneIds() {
        return AVAILABLE_ZONE_IDS;
    }

    static int indexOf(String zoneId) {
        int index = AVAILABLE_ZONE_IDS.indexOf(normalizeZoneId(zoneId));
        if (index >= 0) {
            return index;
        }

        return AVAILABLE_ZONE_IDS.indexOf(DEFAULT_ZONE_ID);
    }

    static ZoneId zoneId(String zoneId) {
        return ZoneId.of(normalizeZoneId(zoneId));
    }

    static String normalizeZoneId(String zoneId) {
        String mapped = mapWindowsTimeZone(zoneId);
        if (mapped == null || mapped.trim().isEmpty()) {
            return DEFAULT_ZONE_ID;
        }

        try {
            return ZoneId.of(mapped.trim()).getId();
        } catch (RuntimeException ignored) {
            return DEFAULT_ZONE_ID;
        }
    }

    private static List<String> buildAvailableZoneIds() {
        List<String> zones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(zones);
        if (!zones.contains(DEFAULT_ZONE_ID)) {
            zones.add(DEFAULT_ZONE_ID);
            Collections.sort(zones);
        }
        return Collections.unmodifiableList(zones);
    }

    private static String mapWindowsTimeZone(String id) {
        if (id == null) {
            return "";
        }

        if ("E. South America Standard Time".equalsIgnoreCase(id.trim())) {
            return DEFAULT_ZONE_ID;
        }

        return id;
    }
}
