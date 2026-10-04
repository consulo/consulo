// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The formatter of the grid model: values are keyed by their class. It reads neither the number format settings nor the custom
 * date and time patterns of the grid settings, so numbers keep their exact text and dates and times use the intrinsic formats of
 * {@code FormatterCreator}:
 * <ul>
 * <li>a date - {@code java.sql.Date}, {@link LocalDate} - as {@code yyyy-MM-dd}</li>
 * <li>a time - {@link Time}, {@link LocalTime} - as {@code HH:mm:ss}</li>
 * <li>a timestamp - {@link Timestamp}, {@link LocalDateTime} - as {@code yyyy-MM-dd HH:mm:ss}</li>
 * <li>a value with an offset - {@link OffsetDateTime}, {@link ZonedDateTime}, {@link OffsetTime} - with {@code +HH:MM}
 * after it, {@code +H:MM} in the {@link ObjectFormatterMode#NORMALIZE} mode</li>
 * </ul>
 * A time is followed by the fraction of a second, see {@link #calculateScale}, and a date before Christ by {@code BC}. A
 * {@link Date} which is not one of the {@code java.sql} classes is formatted by the {@link GridTypeKind} of its column, see
 * {@link #dateToString}.
 * <p/>
 * The {@code java.time} values are formatted as above too, and {@code byte[]} as {@code 0x} and hex.
 */
public class BaseObjectFormatter implements ObjectFormatter {
    public static final int MAX_ARRAY_SIZE = 100;
    public static final int MAX_BINARY_DISPLAY_BYTES = 256;
    /**
     * The scale of a column which does not tell it: the fraction of a second is printed with as many digits as the value
     * needs, and not at all for a whole second.
     */
    public static final int UNKNOWN_SCALE = -1;

    private static final int MAX_SCALE = 9;
    private static final String ERA_PATTERN = "[ ][GGG]";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * {@link DateTimeFormatter} is immutable, and there are at most a few hundred keys.
     */
    private static final ConcurrentMap<TemporalFormatKey, DateTimeFormatter> TEMPORAL_FORMATTERS = new ConcurrentHashMap<>();

    private enum TemporalFormat {
        DATE,
        TIME,
        TIMESTAMP,
        ZONED_TIME,
        ZONED_TIMESTAMP
    }

    private record TemporalFormatKey(TemporalFormat format, int scale, boolean normalize, boolean era) {
    }

    @Override
    public @Nullable String objectToString(@Nullable Object o, GridColumn column, ObjectFormatterConfig config) {
        if (o == null) {
            return null;
        }
        if (o instanceof ReservedCellValue reserved) {
            if (config.getMode() == ObjectFormatterMode.DISPLAY) {
                return reserved.getDisplayName();
            }
            return reserved == ReservedCellValue.NULL || reserved == ReservedCellValue.UNSET ? null : reserved.getSqlName();
        }
        if (o instanceof String s) {
            return s;
        }
        if (o instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (o instanceof Number || o instanceof Boolean || o instanceof UUID) {
            return o.toString();
        }
        if (o instanceof Date || o instanceof TemporalAccessor) {
            try {
                String result = temporalToString(o, column, config);
                if (result != null) {
                    return result;
                }
            }
            catch (DateTimeException e) {
                // a value the format cannot print - like the year 999999999 a driver gives for infinity - is shown by its
                // own text, as for a failed conversion
                return String.valueOf(o);
            }
        }
        if (o instanceof byte[] bytes) {
            return toHexString(bytes, config.getMode() == ObjectFormatterMode.DISPLAY && !config.isAllowedShowBigObjects());
        }
        if (o instanceof char[] chars) {
            return new String(chars);
        }
        if (o instanceof Map<?, ?> || o instanceof List<?>) {
            StringBuilder builder = new StringBuilder();
            appendJson(builder, o);
            return builder.toString();
        }
        if (o instanceof Object[] array) {
            return arrayToString(array, column, config);
        }
        return String.valueOf(o);
    }

    /**
     * @return the text of a date or time value, or {@code null} for a temporal value of another kind - a {@code Year},
     * a date of another chronology - which is shown by its {@code toString()}
     */
    protected @Nullable String temporalToString(Object o, GridColumn column, ObjectFormatterConfig config) {
        // the java.sql classes extend java.util.Date, so they go first
        if (o instanceof Timestamp timestamp) {
            return timestampToString(timestamp, column, config);
        }
        if (o instanceof Time time) {
            return timeToString(time, column, config);
        }
        if (o instanceof java.sql.Date date) {
            // a java.sql.Date has no time of day, whatever column it comes from
            LocalDate localDate = toLocalDateTime(date, -1, getZoneId(column, config)).toLocalDate();
            return formatTemporal(TemporalFormat.DATE, localDate, column, config);
        }
        if (o instanceof Date date) {
            return dateToString(date, column, config);
        }
        if (o instanceof LocalDate date) {
            return formatTemporal(TemporalFormat.DATE, date, column, config);
        }
        if (o instanceof LocalTime time) {
            return formatTemporal(TemporalFormat.TIME, time, column, config);
        }
        if (o instanceof LocalDateTime dateTime) {
            return formatTemporal(TemporalFormat.TIMESTAMP, dateTime, column, config);
        }
        if (o instanceof OffsetDateTime || o instanceof ZonedDateTime) {
            // a ZonedDateTime is shown by its offset, as the zoned timestamp format does
            return formatTemporal(TemporalFormat.ZONED_TIMESTAMP, (TemporalAccessor) o, column, config);
        }
        if (o instanceof OffsetTime time) {
            return formatTemporal(TemporalFormat.ZONED_TIME, time, column, config);
        }
        if (o instanceof Instant instant) {
            // an instant has no offset of its own, so it is shown in the zone of a Timestamp of a column with a time zone
            return formatTemporal(TemporalFormat.ZONED_TIMESTAMP, instant.atZone(getZoneId(column, config)), column, config);
        }
        return null;
    }

    /**
     * A timestamp of a column {@link GridTypeKind#TIMESTAMP_TZ with a time zone} is a moment, so it is shown with the
     * offset of {@link #getZoneId the zone}.
     */
    protected String timestampToString(Timestamp o, GridColumn column, ObjectFormatterConfig config) {
        ZoneId zone = getZoneId(column, config);
        if (column.getType().getKind() == GridTypeKind.TIMESTAMP_TZ) {
            return formatTemporal(TemporalFormat.ZONED_TIMESTAMP, toOffsetDateTime(o, o.getNanos(), zone), column, config);
        }
        return formatTemporal(TemporalFormat.TIMESTAMP, toLocalDateTime(o, o.getNanos(), zone), column, config);
    }

    protected String timeToString(Time o, GridColumn column, ObjectFormatterConfig config) {
        // unlike Time.toLocalTime() it keeps the milliseconds a Time may have
        LocalTime time = toLocalDateTime(o, -1, getZoneId(column, config)).toLocalTime();
        return formatTemporal(TemporalFormat.TIME, time, column, config);
    }

    /**
     * The format is picked by the {@link GridTypeKind} of the column - a timestamp, a time or else a date: a column has no class
     * name the driver gives.
     */
    protected String dateToString(Date o, GridColumn column, ObjectFormatterConfig config) {
        ZoneId zone = getZoneId(column, config);
        return switch (column.getType().getKind()) {
            case TIMESTAMP -> formatTemporal(TemporalFormat.TIMESTAMP, toLocalDateTime(o, -1, zone), column, config);
            case TIMESTAMP_TZ -> formatTemporal(TemporalFormat.ZONED_TIMESTAMP, toOffsetDateTime(o, -1, zone), column, config);
            case TIME -> formatTemporal(TemporalFormat.TIME, toLocalDateTime(o, -1, zone).toLocalTime(), column, config);
            default -> formatTemporal(TemporalFormat.DATE, toLocalDateTime(o, -1, zone).toLocalDate(), column, config);
        };
    }

    /**
     * The zone a {@link Date} - so every {@code java.sql} value - is shown in. The grid settings are not reachable from here, and
     * a driver runs in the zone of the IDE, so it is the system zone.
     */
    protected ZoneId getZoneId(GridColumn column, ObjectFormatterConfig config) {
        return ZoneId.systemDefault();
    }

    /**
     * The number of digits of the fraction of a second. A time or timestamp column which knows its scale gives it, and any other
     * column gives {@link #UNKNOWN_SCALE}.
     */
    protected int calculateScale(GridColumn column, ObjectFormatterConfig config) {
        GridTypeKind kind = column.getType().getKind();
        if (column instanceof SizeProvider sizeProvider
            && (kind == GridTypeKind.TIME || kind == GridTypeKind.TIMESTAMP || kind == GridTypeKind.TIMESTAMP_TZ)) {
            int scale = sizeProvider.getScale();
            if (scale >= 0) {
                return Math.min(scale, MAX_SCALE);
            }
        }
        return UNKNOWN_SCALE;
    }

    private String formatTemporal(TemporalFormat format, TemporalAccessor value, GridColumn column, ObjectFormatterConfig config) {
        boolean zoned = format == TemporalFormat.ZONED_TIME || format == TemporalFormat.ZONED_TIMESTAMP;
        int scale = 0;
        if (format != TemporalFormat.DATE) {
            // a subclass may give any number - DateTimeFormatterBuilder takes at most 9 digits, and the cache stays small
            int columnScale = calculateScale(column, config);
            scale = columnScale < 0 ? UNKNOWN_SCALE : Math.min(columnScale, MAX_SCALE);
        }
        TemporalFormatKey key = new TemporalFormatKey(
            format,
            scale,
            zoned && config.getMode() == ObjectFormatterMode.NORMALIZE,
            value.isSupported(ChronoField.ERA) && value.get(ChronoField.ERA) == 0
        );
        return TEMPORAL_FORMATTERS.computeIfAbsent(key, BaseObjectFormatter::createTemporalFormatter).format(value);
    }

    private static DateTimeFormatter createTemporalFormatter(TemporalFormatKey key) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        TemporalFormat format = key.format();
        if (format == TemporalFormat.DATE) {
            appendDate(builder);
        }
        else {
            if (format == TemporalFormat.TIMESTAMP || format == TemporalFormat.ZONED_TIMESTAMP) {
                appendDate(builder);
                builder.appendPattern("[ ]");
            }
            appendTime(builder);
            appendFraction(builder, key.scale());
            if (format == TemporalFormat.ZONED_TIME || format == TemporalFormat.ZONED_TIMESTAMP) {
                // the first of the offset formats of a zoned value - the others are only for parsing
                String hourPattern = key.normalize() ? "H" : "HH";
                builder.appendPattern("[ ]");
                builder.appendOffset("+" + hourPattern + ":MM:ss", "+00:00");
            }
        }
        if (key.era()) {
            builder.appendPattern(ERA_PATTERN);
        }
        return builder.toFormatter(Locale.US);
    }

    private static DateTimeFormatterBuilder appendDate(DateTimeFormatterBuilder builder) {
        return builder.parseLenient()
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 7, SignStyle.NEVER)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .parseStrict();
    }

    private static DateTimeFormatterBuilder appendTime(DateTimeFormatterBuilder builder) {
        return builder.parseLenient()
            .optionalStart().appendValue(ChronoField.HOUR_OF_DAY, 2).optionalEnd()
            .optionalStart().appendLiteral(":").optionalEnd()
            .optionalStart().appendValue(ChronoField.MINUTE_OF_HOUR, 2).optionalEnd()
            .optionalStart().appendLiteral(":").optionalEnd()
            .optionalStart().appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
            .parseStrict();
    }

    private static void appendFraction(DateTimeFormatterBuilder builder, int scale) {
        if (scale > 0) {
            builder.parseLenient().appendFraction(ChronoField.NANO_OF_SECOND, scale, scale, true).parseStrict();
        }
        else if (scale < 0) {
            builder.appendFraction(ChronoField.NANO_OF_SECOND, 0, MAX_SCALE, true);
        }
    }

    /**
     * The fields are taken from the calendar of {@link Date}, as a {@code SimpleDateFormat} prints them, so a date before the
     * Gregorian reform keeps its Julian day.
     *
     * @param nanos the nanoseconds of the value, or -1 to take the milliseconds of the date
     */
    private static LocalDateTime toLocalDateTime(Date date, int nanos, ZoneId zone) {
        Calendar calendar = toCalendar(date, zone);
        int nanoOfSecond = nanos >= 0 ? nanos : calendar.get(Calendar.MILLISECOND) * 1_000_000;
        int yearOfEra = calendar.get(Calendar.YEAR);
        int year = calendar.get(Calendar.ERA) == GregorianCalendar.BC ? 1 - yearOfEra : yearOfEra;
        int month = calendar.get(Calendar.MONTH) + 1;
        // a Julian day the ISO calendar does not have, like the 29th of February 1500, becomes the last day of the month,
        // as the smart resolver of the parser does
        int day = Math.min(calendar.get(Calendar.DAY_OF_MONTH), YearMonth.of(year, month).lengthOfMonth());
        return LocalDateTime.of(
            year,
            month,
            day,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
            nanoOfSecond
        );
    }

    private static OffsetDateTime toOffsetDateTime(Date date, int nanos, ZoneId zone) {
        Calendar calendar = toCalendar(date, zone);
        int offsetMillis = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
        return toLocalDateTime(date, nanos, zone).atOffset(ZoneOffset.ofTotalSeconds(offsetMillis / 1000));
    }

    private static Calendar toCalendar(Date date, ZoneId zone) {
        // a GregorianCalendar whatever the locale - Calendar.getInstance() may give a Buddhist or Japanese one
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone(zone), Locale.ROOT);
        calendar.setTime(date);
        return calendar;
    }

    @Override
    public boolean isStringLiteral(@Nullable GridColumn column, @Nullable Object value, ObjectFormatterMode mode) {
        if (mode == ObjectFormatterMode.JSON) {
            return !(value instanceof Number || value instanceof Boolean || value instanceof Map || value instanceof List);
        }

        return value instanceof String;
    }

    /**
     * Standard SQL: the quote is doubled and a backslash is taken as it is. Escaping both with a backslash is read by only some
     * databases.
     */
    @Override
    public String getStringLiteral(String value, GridColumn column, ObjectFormatterMode mode) {
        return "'" + value.replace("'", "''") + "'";
    }

    protected String arrayToString(Object[] o, GridColumn column, ObjectFormatterConfig config) {
        if (o.length == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int len = Math.min(o.length, config.getMode() == ObjectFormatterMode.DISPLAY ? MAX_ARRAY_SIZE : o.length);
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(",");
            }
            String item = objectToString(o[i], column, config);
            sb.append(item == null ? "null" : item);
        }
        if (len < o.length) {
            sb.append(",...");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toHexString(byte[] bytes, boolean truncate) {
        int length = truncate ? Math.min(bytes.length, MAX_BINARY_DISPLAY_BYTES) : bytes.length;
        StringBuilder builder = new StringBuilder(2 + length * 2 + 1);
        builder.append("0x");
        for (int i = 0; i < length; i++) {
            builder.append(HEX[(bytes[i] >> 4) & 0xF]).append(HEX[bytes[i] & 0xF]);
        }
        if (length < bytes.length) {
            builder.append('…');
        }
        return builder.toString();
    }

    private static void appendJson(StringBuilder builder, @Nullable Object value) {
        if (value == null) {
            builder.append("null");
        }
        else if (value instanceof Map<?, ?> map) {
            builder.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                appendJsonString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                appendJson(builder, entry.getValue());
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
        }
        else if (value instanceof Collection<?> collection) {
            builder.append('[');
            Iterator<?> iterator = collection.iterator();
            while (iterator.hasNext()) {
                appendJson(builder, iterator.next());
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append(']');
        }
        else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        }
        else {
            appendJsonString(builder, String.valueOf(value));
        }
    }

    private static void appendJsonString(StringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(c);
            }
        }
        builder.append('"');
    }
}
