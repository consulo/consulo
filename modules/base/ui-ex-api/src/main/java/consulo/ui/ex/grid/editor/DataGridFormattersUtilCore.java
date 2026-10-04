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
package consulo.ui.ex.grid.editor;

import consulo.logging.Logger;
import consulo.ui.ex.grid.DataGridObjectFormatterConfig;
import consulo.ui.ex.grid.DataGridSettings;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ObjectFormatterConfig;
import consulo.ui.grid.editor.BoundaryValueResolver;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

import static consulo.ui.ex.grid.editor.FormatsCache.LOCAL_DATE_FORMAT_PROVIDER;
import static consulo.ui.ex.grid.editor.FormatsCache.LOCAL_DATE_WITH_MILLI_FORMAT_PROVIDER;
import static consulo.ui.ex.grid.editor.FormatsCache.OFFSET_DATE_TIME_FORMAT_PROVIDER;
import static consulo.ui.ex.grid.editor.FormatsCache.SHORT_TIMESTAMP_FORMAT_PROVIDER;
import static consulo.ui.ex.grid.editor.FormatsCache.SIMPLE_DATE_FORMAT_PROVIDER;
import static consulo.ui.ex.grid.editor.FormatsCache.SIMPLE_TIMESTAMP_FORMAT_PROVIDER;
import static consulo.ui.ex.grid.editor.FormatsCache.TIMESTAMP_WITH_MILLI_FORMAT_PROVIDER;
import static consulo.ui.ex.grid.editor.FormatterCreator.getTimestampKey;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * The settings of a config are read with {@link DataGridObjectFormatterConfig#getSettings} - a config of {@code consulo.ui.grid}
 * does not carry them.
 */
public final class DataGridFormattersUtilCore {
    public static final LocalDate START_DATE = LocalDate.of(1970, 1, 1);

    private static final Logger LOG = Logger.getInstance(DataGridFormattersUtilCore.class);
    private static final double START_JULIAN_DATE = 2440587.5;
    private static final int MS_PER_DAY = 60 * 60 * 24 * 1000;
    private static final int DEFAULT_ERA = 1;

    private DataGridFormattersUtilCore() {
    }

    public static int getEra(TemporalAccessor date) {
        return date.isSupported(ERA) ? date.get(ERA) : DEFAULT_ERA;
    }

    public static synchronized Timestamp fromOffsetDateTime(OffsetDateTime offsetDateTime,
                                                            FormatsCache formatsCache,
                                                            FormatterCreator creator,
                                                            @Nullable ObjectFormatterConfig config) {
        DateFormat format = formatsCache.get(SIMPLE_TIMESTAMP_FORMAT_PROVIDER, creator);
        format.setTimeZone(getTimeZoneOrDefault(config));
        try {
            DateTimeFormatter offsetDateTimeFormatter = formatsCache.get(OFFSET_DATE_TIME_FORMAT_PROVIDER, creator);
            Date date = (Date) format.parseObject(offsetDateTimeFormatter.format(offsetDateTime));
            Timestamp timestamp = new Timestamp(date.getTime());
            timestamp.setNanos(offsetDateTime.getNano());
            return timestamp;
        }
        catch (ParseException e) {
            LOG.warn(e);
        }
        return Timestamp.valueOf(offsetDateTime.atZoneSameInstant(getZoneIdOrDefault(config)).toLocalDateTime());
    }

    public static TimeZone getDefaultTimeZone() {
        return TimeZone.getTimeZone("UTC");
    }

    public static ZoneOffset getLocalTimeOffset() {
        return ZonedDateTime.of(LocalDateTime.of(1970, 1, 1, 0, 0, 1), ZoneId.systemDefault()).getOffset();
    }

    public static ZoneOffset getDefaultOffset() {
        return ZoneOffset.UTC;
    }

    public static synchronized OffsetDateTime fromTimestamp(Timestamp timestamp,
                                                            FormatsCache formatsCache,
                                                            FormatterCreator formatterCreator,
                                                            @Nullable ObjectFormatterConfig config) {
        DateFormat format = formatsCache.get(SIMPLE_TIMESTAMP_FORMAT_PROVIDER, formatterCreator);
        format.setTimeZone(getDefaultTimeZone());
        ZoneOffset zoneOffset = getZoneOffsetOrDefault(config, timestamp.toInstant());
        try {
            return formatsCache.get(OFFSET_DATE_TIME_FORMAT_PROVIDER, formatterCreator)
                .parse(format.format(timestamp), OffsetDateTime::from)
                .withNano(timestamp.getNanos())
                .withOffsetSameInstant(zoneOffset);
        }
        catch (DateTimeParseException e) {
            LOG.warn(e);
        }
        return timestamp.toLocalDateTime()
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .withOffsetSameInstant(zoneOffset);
    }

    public static synchronized LocalDateTime fromDateToLocalDateTime(Date date,
                                                                     FormatsCache formatsCache,
                                                                     FormatterCreator formatterCreator,
                                                                     @Nullable ObjectFormatterConfig config) {
        DateFormat format = formatsCache.get(TIMESTAMP_WITH_MILLI_FORMAT_PROVIDER, formatterCreator);
        format.setTimeZone(getTimeZoneOrDefault(config));
        try {
            return formatsCache.get(LOCAL_DATE_WITH_MILLI_FORMAT_PROVIDER, formatterCreator)
                .parse(formatsCache.get(TIMESTAMP_WITH_MILLI_FORMAT_PROVIDER, formatterCreator).format(date), LocalDateTime::from);
        }
        catch (DateTimeParseException e) {
            LOG.warn(e);
        }
        Instant instant = date.toInstant();
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
            .atOffset(getZoneOffsetOrDefault(config, instant))
            .toLocalDateTime();
    }

    public static boolean isEmptyTime(TemporalAccessor temporalAccessor) {
        return (!temporalAccessor.isSupported(HOUR_OF_DAY) || temporalAccessor.get(HOUR_OF_DAY) == 0) &&
            (!temporalAccessor.isSupported(MINUTE_OF_HOUR) || temporalAccessor.get(MINUTE_OF_HOUR) == 0) &&
            (!temporalAccessor.isSupported(SECOND_OF_MINUTE) || temporalAccessor.get(SECOND_OF_MINUTE) == 0) &&
            (!temporalAccessor.isSupported(MILLI_OF_SECOND) || temporalAccessor.get(MILLI_OF_SECOND) == 0);
    }

    public static synchronized LocalDate fromDate(Date date, FormatsCache formatsCache, FormatterCreator formatterCreator) {
        DateFormat format = formatsCache.get(SIMPLE_DATE_FORMAT_PROVIDER, formatterCreator);
        format.setTimeZone(getDefaultTimeZone());
        try {
            return formatsCache.get(LOCAL_DATE_FORMAT_PROVIDER, formatterCreator).parse(format.format(date), LocalDate::from);
        }
        catch (DateTimeParseException e) {
            LOG.warn(e);
        }
        return date instanceof java.sql.Date ?
            ((java.sql.Date) date).toLocalDate() :
            LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)
                .atOffset(getDefaultOffset())
                .toLocalDate();
    }

    public static synchronized Date fromLocalDateTime(LocalDateTime dateTime,
                                                      FormatsCache formatsCache,
                                                      FormatterCreator formatterCreator,
                                                      @Nullable ObjectFormatterConfig config) {
        DateFormat format = formatsCache.get(TIMESTAMP_WITH_MILLI_FORMAT_PROVIDER, formatterCreator);
        format.setTimeZone(getTimeZoneOrDefault(config));
        try {
            return (Date) format.parseObject(formatsCache.get(LOCAL_DATE_WITH_MILLI_FORMAT_PROVIDER, formatterCreator).format(dateTime));
        }
        catch (ParseException e) {
            LOG.warn(e);
        }
        return Date.from(dateTime.atZone(getZoneIdOrDefault(config)).toInstant());
    }

    public static synchronized Date fromLocalDate(LocalDate date, FormatsCache formatsCache, FormatterCreator formatterCreator) {
        DateFormat format = formatsCache.get(SIMPLE_DATE_FORMAT_PROVIDER, formatterCreator);
        format.setTimeZone(getDefaultTimeZone());
        try {
            return (Date) format.parseObject(formatsCache.get(LOCAL_DATE_FORMAT_PROVIDER, formatterCreator).format(date));
        }
        catch (ParseException e) {
            LOG.warn(e);
        }
        return Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private static @Nullable Date parseDateFromNumber(String s) {
        try {
            return new Date(Long.parseLong(s));
        }
        catch (NumberFormatException ignore) {
        }
        try {
            return new Date(fromJulian(Double.parseDouble(s)));
        }
        catch (NumberFormatException ignore) {
        }
        return null;
    }

    private static long fromJulian(double d) {
        return (long) (d - START_JULIAN_DATE) * MS_PER_DAY;
    }

    private static synchronized Date getUtcDate(FormatsCache formatsCache, FormatterCreator formatterCreator) {
        Date currentDate = new Date();
        DateFormat format = formatsCache.get(SHORT_TIMESTAMP_FORMAT_PROVIDER, formatterCreator);
        format.setTimeZone(TimeZone.getDefault());
        String formattedInLocalTz = format.format(currentDate);
        format.setTimeZone(getDefaultTimeZone());
        try {
            return format.parse(formattedInLocalTz);
        }
        catch (ParseException e) {
            LOG.warn(e);
        }

        return currentDate;
    }

    public static Date getDateFrom(@Nullable Object o,
                                   CoreGrid<GridRow, GridColumn> grid,
                                   ModelIndex<GridColumn> column,
                                   FormatsCache formatsCache,
                                   FormatterCreator formatterCreator) {
        if (o instanceof String) {
            Date fromNumber = parseDateFromNumber((String) o);
            if (fromNumber != null) {
                return fromNumber;
            }
            GridColumn c = grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(column);
            Formatter format = formatterCreator.create(getTimestampKey(c, null, formatsCache));
            try {
                Date res = ObjectUtil.tryCast(format.parse((String) o), Date.class);
                return ObjectUtil.notNull(res, getUtcDate(formatsCache, formatterCreator));
            }
            catch (ParseException e) {
                getUtcDate(formatsCache, formatterCreator);
            }
        }
        return o instanceof Double ? new Date(fromJulian((Double) o)) :
            o instanceof Number ? new Date(((Number) o).longValue()) :
                o instanceof Date ? (Date) o :
                    getUtcDate(formatsCache, formatterCreator);
    }

    public static Date getBoundedValue(Object value, ModelIndex<GridColumn> column, CoreGrid<GridRow, GridColumn> grid) {
        BoundaryValueResolver resolver = GridCellEditorHelper.get(grid).getResolver(grid, column);
        return resolver.bound(value);
    }

    public static @Nullable ZoneId getZoneId(@Nullable ObjectFormatterConfig config) {
        if (config == null) {
            return null;
        }
        DataGridSettings settings = DataGridObjectFormatterConfig.getSettings(config);
        if (settings == null) {
            return null;
        }
        return settings.getEffectiveZoneId();
    }

    public static ZoneId getZoneIdOrDefault(@Nullable ObjectFormatterConfig config) {
        return ObjectUtil.notNull(getZoneId(config), ZoneId.systemDefault());
    }

    public static ZoneOffset getLocalTimeOffset(@Nullable ObjectFormatterConfig config) {
        ZoneId zoneId = getZoneIdOrDefault(config);
        LocalDateTime referenceDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        return ZonedDateTime.of(referenceDateTime, zoneId).getOffset();
    }

    public static @Nullable TimeZone getTimeZoneOrDefault(@Nullable ObjectFormatterConfig config) {
        ZoneId zoneId = getZoneId(config);
        return zoneId != null ? TimeZone.getTimeZone(zoneId) : getDefaultTimeZone();
    }

    public static ZoneOffset getZoneOffsetOrDefault(@Nullable ObjectFormatterConfig config, Instant instant) {
        ZoneId zoneId = getZoneId(config);
        return zoneId != null ? zoneId.getRules().getOffset(instant) : getDefaultOffset();
    }

    public static ZoneOffset getZoneOffsetByEpochOrDefault(@Nullable ObjectFormatterConfig config) {
        return getZoneOffsetOrDefault(config, Instant.now());
    }

    public static OffsetTime adjustOffset(OffsetTime time, @Nullable ZoneId zoneId) {
        if (zoneId != null) {
            ZoneOffset offset = zoneId.getRules().getOffset(time.toLocalTime().atDate(LocalDate.now()));
            return time.withOffsetSameInstant(offset);
        }
        return time;
    }

    public static Temporal adjustTimeZone(OffsetDateTime dateTime, @Nullable ZoneId zoneId) {
        return zoneId != null ? dateTime.atZoneSameInstant(zoneId) : dateTime;
    }

    public static OffsetDateTime adjustOffset(OffsetDateTime dateTime, @Nullable ZoneId zoneId) {
        return zoneId != null ? dateTime.atZoneSameInstant(zoneId).toOffsetDateTime() : dateTime;
    }

    public static OffsetDateTime toOffsetDateTime(LocalDateTime dateTime, @Nullable ZoneId zoneId) {
        return dateTime.atZone(ObjectUtil.notNull(zoneId, ZoneId.systemDefault())).toOffsetDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(TemporalAccessor value, @Nullable ZoneId zoneId) {
        return value instanceof LocalDateTime dateTime
            ? toOffsetDateTime(dateTime, zoneId)
            : adjustOffset((OffsetDateTime) value, zoneId);
    }
}
