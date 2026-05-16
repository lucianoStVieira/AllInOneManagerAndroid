package com.allinonemanager.android;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TimeZoneSupport {
    static final String DEFAULT_ZONE_ID = "America/Sao_Paulo";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> AVAILABLE_ZONE_IDS = buildAvailableZoneIds();

    private TimeZoneSupport() {
    }

    static List<TimeZoneOption> availableZoneOptions() {
        Instant now = Instant.now();
        List<TimeZoneOption> options = new ArrayList<>();
        for (String zoneId : AVAILABLE_ZONE_IDS) {
            options.add(new TimeZoneOption(zoneId, displayLabel(zoneId, now)));
        }
        return Collections.unmodifiableList(options);
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
        if (!zones.contains(DEFAULT_ZONE_ID)) {
            zones.add(DEFAULT_ZONE_ID);
        }
        Instant now = Instant.now();
        Collections.sort(zones, (left, right) -> {
            int leftOffset = ZoneId.of(left).getRules().getOffset(now).getTotalSeconds();
            int rightOffset = ZoneId.of(right).getRules().getOffset(now).getTotalSeconds();
            int byOffset = Integer.compare(leftOffset, rightOffset);
            return byOffset == 0 ? left.compareTo(right) : byOffset;
        });
        return Collections.unmodifiableList(zones);
    }

    private static String displayLabel(String zoneId, Instant now) {
        ZoneId zone = zoneId(zoneId);
        ZoneOffset offset = zone.getRules().getOffset(now);
        return "(" + formatOffset(offset) + ") "
                + zone.getId()
                + " - "
                + TIME_FORMAT.format(now.atZone(zone));
    }

    private static String formatOffset(ZoneOffset offset) {
        int totalMinutes = offset.getTotalSeconds() / 60;
        char sign = totalMinutes < 0 ? '-' : '+';
        int absoluteMinutes = Math.abs(totalMinutes);
        int hours = absoluteMinutes / 60;
        int minutes = absoluteMinutes % 60;
        return "UTC" + sign + twoDigits(hours) + ":" + twoDigits(minutes);
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
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

    static final class TimeZoneOption {
        private final String zoneId;
        private final String label;

        TimeZoneOption(String zoneId, String label) {
            this.zoneId = normalizeZoneId(zoneId);
            this.label = label;
        }

        String zoneId() {
            return zoneId;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
