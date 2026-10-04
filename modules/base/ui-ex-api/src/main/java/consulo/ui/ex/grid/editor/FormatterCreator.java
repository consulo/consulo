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

import consulo.localize.LocalizeValue;
import consulo.ui.ex.grid.DataGridObjectFormatterConfig;
import consulo.ui.ex.grid.DataGridSettings;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ObjectFormatter;
import consulo.ui.grid.ObjectFormatterConfig;
import consulo.ui.grid.ObjectFormatterMode;
import consulo.ui.grid.editor.BoundaryValueResolver;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static consulo.ui.ex.grid.editor.DataGridFormattersUtilCore.adjustOffset;
import static consulo.ui.ex.grid.editor.DataGridFormattersUtilCore.adjustTimeZone;
import static consulo.ui.ex.grid.editor.DataGridFormattersUtilCore.getZoneId;
import static consulo.ui.ex.grid.editor.DataGridFormattersUtilCore.toOffsetDateTime;

/**
 * Notes:
 * <ul>
 * <li>there is no extension point for the creators: {@link #get} gives the creator {@link #set installed} on the grid, or a plain
 * {@code FormatterCreator} for each grid;</li>
 * <li>the priority type of {@link #getDecimalWithPriorityTypeKey} is a {@link GridTypeKind};</li>
 * <li>the settings of a config are read with {@link DataGridObjectFormatterConfig#getSettings};</li>
 * <li>error messages are {@link LocalizeValue}s.</li>
 * </ul>
 */
public class FormatterCreator {
    public static final int MAX_FRACTION_DIGITS = 340; // see java.text.DecimalFormat.setMaximumFractionDigits
    public static final FormatterKey<SimpleDateFormat> SIMPLE_TIMESTAMP_FORMATTER_KEY =
        new FormatterKey<>("SIMPLE_TIMESTAMP_FORMATTER_KEY");
    public static final FormatterKey<SimpleDateFormat> SHORT_TIMESTAMP_FORMATTER_KEY = new FormatterKey<>("SHORT_TIMESTAMP_FORMATTER_KEY");
    public static final FormatterKey<SimpleDateFormat> TIMESTAMP_WITH_MILLI_FORMATTER_KEY =
        new FormatterKey<>("TIMESTAMP_WITH_MILLI_FORMATTER_KEY");
    public static final FormatterKey<SimpleDateFormat> SIMPLE_DATE_FORMATTER_KEY = new FormatterKey<>("SIMPLE_DATE_FORMATTER_KEY");
    public static final FormatterKey<DateTimeFormatter> LOCAL_DATE_WITH_MILLI_FORMATTER_KEY = new FormatterKey<>("LOCAL_DATE_WITH_MILLI");
    public static final FormatterKey<DateTimeFormatter> LOCAL_DATE_FORMATTER_KEY = new FormatterKey<>("LOCAL_DATE");
    public static final FormatterKey<DateTimeFormatter> OFFSET_DATE_TIME_FORMATTER_KEY = new FormatterKey<>("OFFSET_DATE_TIME");

    private static final Key<FormatterCreator> FORMATTER_CREATOR_KEY = Key.create("FORMATTER_CREATOR_KEY");
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String ERA_PATTERN = "[ ][GGG]";
    private static final String INT_FORMATTER_KEY = "INT_FORMATTER_KEY";
    private static final String LONG_FORMATTER_KEY = "LONG_FORMATTER_KEY";
    private static final String BIG_INT_FORMATTER_KEY = "BIG_INT_FORMATTER_KEY";
    private static final String FLOAT_FORMATTER_KEY = "FLOAT_FORMATTER_KEY";
    private static final String DOUBLE_FORMATTER_KEY = "DOUBLE_FORMATTER_KEY";
    private static final String DECIMAL_WITH_PRIORITY_TYPE = "DECIMAL_WITH_PRIORITY_TYPE";
    private static final String TIMESTAMP_WITH_SCALE_TYPE = "TIMESTAMP_WITH_SCALE_TYPE";
    private static final String TIME_FORMATTER_KEY = "TIME_FORMATTER_KEY";
    private static final String TIMESTAMP_FORMATTER_KEY = "TIMESTAMP_FORMATTER_KEY";
    private static final String SHORT_ERA_ZONED_TIMESTAMP = "SHORT_ERA_ZONED_TIMESTAMP";
    private static final String ERA_TIMESTAMP = "ERA_TIMESTAMP";
    private static final String ZONED_TIME_FORMATTER_KEY = "ZONED_TIME_FORMATTER_KEY";
    private static final String ZONED_TIMESTAMP_FORMATTER_KEY = "ZONED_TIMESTAMP_FORMATTER_KEY";
    private static final String DATE_FORMATTER_KEY = "DATE_FORMATTER_KEY";
    private static final String DECIMAL_FORMATTER_KEY = "DECIMAL_FORMATTER_KEY";
    private static final String BIG_DECIMAL_FORMATTER_KEY = "BIG_DECIMAL_FORMATTER_KEY";

    public static FormatterCreator get(CoreGrid<GridRow, GridColumn> grid) {
        FormatterCreator creator = grid.getUserData(FORMATTER_CREATOR_KEY);
        if (creator == null) {
            creator = new FormatterCreator();
            grid.putUserData(FORMATTER_CREATOR_KEY, creator);
        }
        return creator;
    }

    /**
     * A data source whose values need other formats - a database, for example - installs a subclass on its grid.
     */
    public static void set(CoreGrid<GridRow, GridColumn> grid, @Nullable FormatterCreator creator) {
        grid.putUserData(FORMATTER_CREATOR_KEY, creator);
    }

    public static FormatterKey<NumberFormatter> getIntKey(@Nullable ObjectFormatterConfig config) {
        return add(new FormatterKey<>(INT_FORMATTER_KEY), config);
    }

    public static FormatterKey<NumberFormatter> getLongKey(@Nullable ObjectFormatterConfig config) {
        return add(new FormatterKey<>(LONG_FORMATTER_KEY), config);
    }

    public static FormatterKey<NumberFormatter> getBigIntKey(@Nullable ObjectFormatterConfig config) {
        return add(new FormatterKey<>(BIG_INT_FORMATTER_KEY), config);
    }

    public static FormatterKey<NumberFormatter> getFloatKey(@Nullable ObjectFormatterConfig config) {
        return add(new FormatterKey<>(FLOAT_FORMATTER_KEY), config);
    }

    public static FormatterKey<NumberFormatter> getDoubleKey(@Nullable ObjectFormatterConfig config) {
        return add(new FormatterKey<>(DOUBLE_FORMATTER_KEY), config);
    }

    public static FormatterKey<NumberFormatter> getDecimalWithPriorityTypeKey(GridTypeKind type, @Nullable ObjectFormatterConfig config) {
        return add(add(new FormatterKey<>(DECIMAL_WITH_PRIORITY_TYPE), type), config);
    }

    public static FormatterKey<Formatter> getTimeKey(@Nullable GridColumn column,
                                                     @Nullable ObjectFormatterConfig config,
                                                     FormatsCache formatsCache) {
        return add(add(add(new FormatterKey<>(TIME_FORMATTER_KEY), column), config), formatsCache);
    }

    public static FormatterKey<Formatter> getTimestampKey(int scale, FormatsCache formatsCache) {
        return add(add(new FormatterKey<>(TIMESTAMP_WITH_SCALE_TYPE), scale), formatsCache);
    }

    public static FormatterKey<Formatter> getTimestampKey(@Nullable GridColumn column,
                                                          @Nullable ObjectFormatterConfig config,
                                                          FormatsCache formatsCache) {
        return add(add(add(new FormatterKey<>(TIMESTAMP_FORMATTER_KEY), column), config), formatsCache);
    }

    public static FormatterKey<Formatter> getShortEraZonedTimestampKey(@Nullable GridColumn column,
                                                                       @Nullable ObjectFormatterConfig config,
                                                                       FormatsCache formatsCache) {
        return add(add(add(new FormatterKey<>(SHORT_ERA_ZONED_TIMESTAMP), column), config), formatsCache);
    }

    public static FormatterKey<Formatter> getEraTimestampKey(@Nullable GridColumn column,
                                                             @Nullable ObjectFormatterConfig config,
                                                             FormatsCache formatsCache) {
        return add(add(add(new FormatterKey<>(ERA_TIMESTAMP), column), config), formatsCache);
    }

    public static FormatterKey<CompositeFormatter> getZonedTimeKey(@Nullable GridColumn column,
                                                                   @Nullable ObjectFormatterConfig config,
                                                                   FormatsCache formatsCache) {
        return add(add(add(new FormatterKey<>(ZONED_TIME_FORMATTER_KEY), column), config), formatsCache);
    }

    public static FormatterKey<CompositeFormatter> getZonedTimestampKey(@Nullable GridColumn column,
                                                                        @Nullable ObjectFormatterConfig config,
                                                                        FormatsCache formatsCache) {
        return add(add(add(new FormatterKey<>(ZONED_TIMESTAMP_FORMATTER_KEY), column), config), formatsCache);
    }

    public static FormatterKey<Formatter> getDateKey(@Nullable GridColumn column,
                                                     @Nullable ObjectFormatterConfig config,
                                                     FormatsCache formatsCache) {
        return add(add(add(new FormatterKey<>(DATE_FORMATTER_KEY), column), config), formatsCache);
    }

    public static FormatterKey<NumberFormatter> getDecimalKey(@Nullable GridColumn column, @Nullable ObjectFormatterConfig config) {
        return add(add(new FormatterKey<>(DECIMAL_FORMATTER_KEY), column), config);
    }

    @SuppressWarnings("unchecked")
    public <T> T create(FormatterKey<T> key) {
        T res = (T) createInner(key);
        if (res != null) {
            return res;
        }
        throw new IllegalArgumentException("Unknown key: " + key.getName());
    }

    /**
     * The formatter of the key, by its name.
     */
    private @Nullable Object createInner(FormatterKey<?> key) {
        String name = key.getName();
        if (name.equals(INT_FORMATTER_KEY)) {
            return newIntFormat(getFormaterConfig(key));
        }
        if (name.equals(FLOAT_FORMATTER_KEY)) {
            return newFloatFormat(getFormaterConfig(key));
        }
        if (name.equals(DOUBLE_FORMATTER_KEY)) {
            return newDoubleFormat(getFormaterConfig(key));
        }
        if (name.equals(LONG_FORMATTER_KEY)) {
            return newLongFormat(getFormaterConfig(key));
        }
        if (name.equals(BIG_INT_FORMATTER_KEY)) {
            return newBigIntFormat(getFormaterConfig(key));
        }
        if (name.equals(DECIMAL_FORMATTER_KEY)) {
            return newDecimalFormat(getColumn(key), getFormaterConfig(key));
        }
        if (name.equals(BIG_DECIMAL_FORMATTER_KEY)) {
            return newBigDecimalFormat(getFormaterConfig(key));
        }
        if (name.equals(DECIMAL_WITH_PRIORITY_TYPE)) {
            return getDecimalFormatWithPriorityType(getTypeKind(key), getFormaterConfig(key));
        }
        if (name.equals(TIMESTAMP_WITH_SCALE_TYPE)) {
            return newTimestampFormat(null, getInt(key), resolver(null), null, getFormatsCache(key));
        }
        if (name.equals(TIMESTAMP_FORMATTER_KEY)) {
            GridColumn column = getColumn(key);
            ObjectFormatterConfig config = getFormaterConfig(key);
            return newTimestampFormat(column, calculateScale(column, config), resolver(column), config, getFormatsCache(key));
        }
        if (name.equals(SHORT_ERA_ZONED_TIMESTAMP)) {
            return newShortEraZonedTimestampFormat(getColumn(key), getFormaterConfig(key), getFormatsCache(key));
        }
        if (name.equals(ERA_TIMESTAMP)) {
            return newEraTimestampFormat(getColumn(key), getFormaterConfig(key), getFormatsCache(key));
        }
        if (name.equals(TIME_FORMATTER_KEY)) {
            return newTimeFormat(getFormaterConfig(key), calculateScale(getColumn(key), getFormaterConfig(key)));
        }
        if (key.is(SIMPLE_TIMESTAMP_FORMATTER_KEY)) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss G Z", Locale.US);
        }
        if (key.is(SHORT_TIMESTAMP_FORMATTER_KEY)) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss G", Locale.US);
        }
        if (key.is(TIMESTAMP_WITH_MILLI_FORMATTER_KEY)) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss G SSS", Locale.US);
        }
        if (key.is(SIMPLE_DATE_FORMATTER_KEY)) {
            return new SimpleDateFormat("yyyy-MM-dd G", Locale.US);
        }
        if (key.is(LOCAL_DATE_WITH_MILLI_FORMATTER_KEY)) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss G SSS", Locale.US);
        }
        if (key.is(LOCAL_DATE_FORMATTER_KEY)) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd G", Locale.US);
        }
        if (key.is(OFFSET_DATE_TIME_FORMATTER_KEY)) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss G ZZZ", Locale.US);
        }
        if (name.equals(ZONED_TIME_FORMATTER_KEY)) {
            return newZonedTimeFormat(getColumn(key), getFormaterConfig(key), getFormatsCache(key));
        }
        if (name.equals(ZONED_TIMESTAMP_FORMATTER_KEY)) {
            return newZonedTimestampFormat(getColumn(key), getFormaterConfig(key), getFormatsCache(key));
        }
        if (name.equals(DATE_FORMATTER_KEY)) {
            return newDateFormatter(getColumn(key), getFormaterConfig(key), getFormatsCache(key));
        }
        return null;
    }

    protected static <T> FormatsCache getFormatsCache(FormatterKey<T> key) {
        return Objects.requireNonNull(key.getUserData(FORMATTER_KEY_FORMATS_CACHE_KEY));
    }

    private static <T> int getInt(FormatterKey<T> key) {
        return Objects.requireNonNull(key.getUserData(FORMATTER_KEY_INT_KEY));
    }

    private static <T> GridTypeKind getTypeKind(FormatterKey<T> key) {
        return Objects.requireNonNull(key.getUserData(FORMATTER_KEY_TYPE_KIND_KEY));
    }

    private static <T> @Nullable GridColumn getColumn(FormatterKey<T> key) {
        return key.getUserData(FORMATTER_KEY_GRID_COLUMN_KEY);
    }

    private static <T> @Nullable ObjectFormatterConfig getFormaterConfig(FormatterKey<T> key) {
        return key.getUserData(FORMATTER_KEY_CONFIG_KEY);
    }

    protected NumberFormatter newDecimalFormat(@Nullable GridColumn column, @Nullable ObjectFormatterConfig config) {
        return configureDecimalFormat(new DecimalFormat(), config);
    }

    protected static NumberFormatter newBigDecimalFormat(@Nullable ObjectFormatterConfig config) {
        NumberFormatter formatter = configureDecimalFormat(new DecimalFormat(), config);
        formatter.setParseBigDecimal(true);
        formatter.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        return formatter;
    }

    protected NumberFormatter newFloatFormat(@Nullable ObjectFormatterConfig config) {
        return configureDecimalFormat(new DecimalFormat() {
            @Override
            public @Nullable Number parse(String text, ParsePosition pos) {
                Number n = super.parse(text, pos);
                return n != null ? n.floatValue() : null;
            }

            @Override
            public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
                // The original value was converted from Float, so we convert it back, obtain a string representation of it,
                // and parse a double value from this representation. Thus we obtain a more precise value from a human point of view.
                // For instance Float.toString(0.99f) == "0.99", whereas Double.toString(0.99f) == "0.9900000095367432".
                double precise = Double.parseDouble(Float.toString((float) number));
                return super.format(precise, result, fieldPosition);
            }
        }, config);
    }

    /**
     * This formatter may return double, int, long, NaN, Infinity
     */
    private NumberFormatter newDoubleFormat(@Nullable ObjectFormatterConfig config) {
        return configureDecimalFormat(new DecimalFormat() {
            @Override
            public @Nullable Number parse(String text, ParsePosition pos) {
                Number n = super.parse(text, pos);
                return n == null ? null :
                    ObjectUtil.notNull(asInt(n), n); // use int if number fits in int
            }
        }, config);
    }

    private NumberFormatter newIntFormat(@Nullable ObjectFormatterConfig config) {
        DecimalFormat format = new DecimalFormat() {
            @Override
            public @Nullable Number parse(String text, ParsePosition pos) {
                Number n = super.parse(text, pos);
                return n != null ? n.intValue() : null;
            }
        };
        NumberFormatter formatter = configureDecimalFormat(format, config);
        formatter.setParseIntegerOnly(true);
        return formatter;
    }

    /**
     * This formatter may return long, NaN, Infinity
     */
    private NumberFormatter newLongFormat(@Nullable ObjectFormatterConfig config) {
        NumberFormatter formatter = configureDecimalFormat(new DecimalFormat() {
            @Override
            public @Nullable Number parse(String text, ParsePosition pos) {
                Number n = super.parse(text, pos);
                return n != null ? n.longValue() : null;
            }
        }, config);
        formatter.setParseIntegerOnly(true);
        return formatter;
    }

    /**
     * This formatter may return BigDecimal that contains integer value, NaN, Infinity
     */
    private NumberFormatter newBigIntFormat(@Nullable ObjectFormatterConfig config) {
        NumberFormatter format = newDecimalFormat(null, config);
        format.setParseIntegerOnly(true);
        format.setParseBigDecimal(true);
        return format;
    }

    /**
     * The kind decides the class a number parses to: {@link GridTypeKind#INTEGER} to an Integer, else a Long;
     * {@link GridTypeKind#FLOAT} to a Double; anything else stays a BigDecimal. A {@link GridTypeKind} has no {@code BIGINT} and one
     * {@link GridTypeKind#FLOAT} for every approximate number.
     */
    private static NumberFormatter getDecimalFormatWithPriorityType(GridTypeKind priorityType, @Nullable ObjectFormatterConfig config) {
        NumberFormatter formatter = configureDecimalFormat(new DecimalFormat() {
            @Override
            public @Nullable Number parse(String text, ParsePosition pos) {
                Number n = super.parse(text, pos);
                return n == null ? null :
                    ObjectUtil.notNull(
                        priorityType == GridTypeKind.INTEGER ? asInt(n) : null, ObjectUtil.notNull(
                            priorityType == GridTypeKind.INTEGER ? asLong(n) : null, ObjectUtil.notNull(
                                priorityType == GridTypeKind.FLOAT ? asDouble(n) : null,
                                n)));
            }
        }, config);
        formatter.setParseBigDecimal(true);
        formatter.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        return formatter;
    }

    private static @Nullable Integer asInt(Number n) {
        try {
            return n instanceof Long && n.intValue() == (long) n ? (Integer) n.intValue() :
                n instanceof BigDecimal ? (Integer) ((BigDecimal) n).intValueExact() : null;
        }
        catch (ArithmeticException ignored) {
            return null;
        }
    }

    private static @Nullable Long asLong(Number n) {
        try {
            return n instanceof Long ? (Long) n :
                n instanceof BigDecimal ? (Long) ((BigDecimal) n).longValueExact() : null;
        }
        catch (ArithmeticException ignored) {
            return null;
        }
    }

    private static @Nullable Double asDouble(Number n) {
        return n instanceof Double
            ? (Double) n
            : n instanceof BigDecimal && n.equals(BigDecimal.valueOf(n.doubleValue())) ? (Double) n.doubleValue() : null;
    }

    private Formatter newZonedTimeFormat(@Nullable GridColumn column,
                                         @Nullable ObjectFormatterConfig config,
                                         FormatsCache formatsCache) {
        Formatter customFormatter =
            newCustomDateTimeFormat(config, DataGridSettings::getEffectiveZonedTimePattern, newZonedTimeDelegate(config));
        if (customFormatter != null) {
            return customFormatter;
        }
        return newZonedTimeFormat(newZonedTimeDelegate(config), calculateScale(column, config), config);
    }

    protected ZonedTimeDelegate<?> newZonedTimeDelegate(@Nullable ObjectFormatterConfig config) {
        return new ZonedTimeDelegate<>() {
            @Override
            protected TemporalAccessor toTemporalAccessor(Object value) {
                if (!(value instanceof OffsetTime time)) {
                    throw new IllegalArgumentException("Value must be of type OffsetTime");
                }
                return adjustOffset(time, getZoneId(config));
            }

            @Override
            protected OffsetTime createFromTemporal(TemporalAccessor value) {
                return adjustOffset(OffsetTime.from(value), getZoneId(config));
            }
        };
    }

    private Formatter newTimeFormat(@Nullable ObjectFormatterConfig config, int scale) {
        return newTimeFormat(config, scale, new TimeDelegate(config));
    }

    public Formatter newTimeFormat(@Nullable ObjectFormatterConfig config, int scale, DateAndTimeFormatterDelegate<?, ?> delegate) {
        Formatter customFormatter = newCustomDateTimeFormat(config, DataGridSettings::getEffectiveTimePattern, delegate);
        if (customFormatter != null) {
            return customFormatter;
        }

        Builders builders = new Builders();
        appendTime(builders);
        if (scale > 0) {
            appendFraction(builders, scale);
        }
        return new DateAndTimeFormatter<>(builders.sb.toString(), toFormatter(builders.fb), delegate);
    }

    protected Formatter newTimestampFormat(@Nullable GridColumn column,
                                           int scale,
                                           BoundaryValueResolver resolver,
                                           @Nullable ObjectFormatterConfig config,
                                           FormatsCache formatsCache) {
        Formatter customFormatter = newCustomTimestampFormat(config, formatsCache);
        if (customFormatter != null) {
            return customFormatter;
        }
        return newIntrinsicTimestampFormat(column, scale, resolver, config, formatsCache);
    }

    protected Formatter newIntrinsicTimestampFormat(@Nullable GridColumn column,
                                                    int scale,
                                                    BoundaryValueResolver resolver,
                                                    @Nullable ObjectFormatterConfig config,
                                                    FormatsCache formatsCache) {
        return checkInfinity(newTimestampFormat(config, scale, formatsCache), resolver);
    }

    private DateAndTimeFormatter<Timestamp, OffsetDateTime> newTimestampFormat(@Nullable ObjectFormatterConfig config,
                                                                               int scale,
                                                                               FormatsCache formatsCache) {
        Builders builders = new Builders();
        appendDate(builders);
        builders.sb.append(' ');
        builders.fb.appendPattern("[ ]");
        appendTime(builders);
        appendFraction(builders, scale);
        return new DateAndTimeFormatter<>(builders.sb.toString(), toFormatter(builders.fb),
            new TimestampDelegate(formatsCache, this, config));
    }

    protected @Nullable Formatter newCustomTimestampFormat(@Nullable ObjectFormatterConfig config, FormatsCache formatsCache) {
        return newCustomDateTimeFormat(config, DataGridSettings::getEffectiveDateTimePattern,
            new TimestampDelegate(formatsCache, this, config));
    }

    protected @Nullable Formatter newCustomDateTimeFormat(@Nullable ObjectFormatterConfig config,
                                                          Function<? super DataGridSettings, @Nullable String> patternGetter,
                                                          DateAndTimeFormatterDelegate<?, ?> delegate) {
        if (config == null || config.getMode() != ObjectFormatterMode.DISPLAY) {
            return null;
        }
        DataGridSettings settings = DataGridObjectFormatterConfig.getSettings(config);
        String pattern = settings != null ? patternGetter.apply(settings) : null;
        if (pattern == null) {
            return null;
        }
        return new DateAndTimeFormatter<>(
            pattern,
            toFormatter(new DateTimeFormatterBuilder().appendPattern(pattern)),
            delegate
        );
    }

    private CompositeFormatter newZonedTimestampFormat(@Nullable GridColumn column,
                                                       @Nullable ObjectFormatterConfig config,
                                                       FormatsCache formatsCache) {
        return newZonedTimestampFormat(resolver(column), calculateScale(column, config), formatsCache, config);
    }

    public CompositeFormatter newZonedTimestampFormat(BoundaryValueResolver resolver,
                                                      int scale,
                                                      FormatsCache formatsCache,
                                                      @Nullable ObjectFormatterConfig config) {
        return newZonedTimestampFormat(new ZonedTimestampDelegate<OffsetDateTime>() {
            @Override
            protected OffsetDateTime toTemporalAccessor(Object value) {
                if (!(value instanceof OffsetDateTime)) {
                    throw new IllegalArgumentException("Value must be type of OffsetDateTime");
                }
                return (OffsetDateTime) value;
            }

            @Override
            protected OffsetDateTime createFromTemporal(OffsetDateTime value) {
                return value;
            }
        }, resolver, scale, config);
    }

    public Formatter newIsoFormatter(DateAndTimeFormatterDelegate<?, ?> delegate) {
        return new DateAndTimeFormatter<>("ISO-8601", DateTimeFormatter.ISO_DATE_TIME, delegate);
    }

    public <T, V extends TemporalAccessor> CompositeFormatter newZonedTimestampFormat(DateAndTimeFormatterDelegate<T, V> delegate,
                                                                                      BoundaryValueResolver resolver,
                                                                                      int scale,
                                                                                      @Nullable ObjectFormatterConfig config) {
        Builders builders = createBuildersForTimestamp(scale);
        List<DateTimeFormatter> formatters = appendTimeZone(builders, config);
        String pattern = builders.sb.toString();
        List<Formatter> mapped = ContainerUtil.map(formatters, f -> eraFormatter(pattern, f, delegate, resolver, false));
        return new CompositeFormatter(LocalizeValue.localizeTODO("Expected timestamp with time zone"), mapped);
    }

    protected Formatter newEraTimestampFormat(@Nullable GridColumn column,
                                              @Nullable ObjectFormatterConfig config,
                                              FormatsCache formatsCache) {
        Builders builders = createBuildersForTimestamp(calculateScale(column, config));
        return checkInfinity(eraFormatter(
            builders.sb.toString(),
            toFormatter(builders.fb),
            new TimestampDelegate(formatsCache, this, config),
            resolver(column),
            omitEmptyTime(column)
        ), resolver(column));
    }

    protected boolean omitEmptyTime(@Nullable GridColumn column) {
        return false;
    }

    protected BoundaryValueResolver resolver(@Nullable GridColumn column) {
        return BoundaryValueResolver.ALWAYS_NULL;
    }

    private Formatter newShortEraZonedTimestampFormat(@Nullable GridColumn column,
                                                      @Nullable ObjectFormatterConfig config,
                                                      FormatsCache formatsCache) {
        Formatter customFormatter = newCustomDateTimeFormat(
            config,
            DataGridSettings::getEffectiveZonedDateTimePattern,
            newShortZonedTimestampDelegate(config)
        );
        if (customFormatter != null) {
            return customFormatter;
        }

        return checkInfinity(newShortEraZonedTimestampFormat(calculateScale(column, config), config, formatsCache), resolver(column));
    }

    protected int calculateScale(@Nullable GridColumn column, @Nullable ObjectFormatterConfig config) {
        return 0;
    }

    private Formatter newShortEraZonedTimestampFormat(int scale, @Nullable ObjectFormatterConfig config, FormatsCache formatsCache) {
        return newShortZonedTimestampFormat(scale, (formatter, pattern) -> eraFormatter(
            pattern,
            formatter,
            newShortZonedTimestampDelegate(config),
            BoundaryValueResolver.ALWAYS_NULL,
            false
        ), config);
    }

    protected ShortZonedTimestampDelegate<?> newShortZonedTimestampDelegate(@Nullable ObjectFormatterConfig config) {
        return new ShortZonedTimestampDelegate<>() {
            @Override
            protected Temporal toTemporalAccessor(Object value) {
                if (!(value instanceof OffsetDateTime dateTime)) {
                    throw new IllegalArgumentException("Value must be of type OffsetDateTime");
                }
                return adjustTimeZone(dateTime, getZoneId(config));
            }

            @Override
            protected Temporal createFromTemporal(TemporalAccessor value) {
                return toOffsetDateTime(value, getZoneId(config));
            }
        };
    }

    protected Formatter newEraDateFormatter(@Nullable ObjectFormatterConfig config,
                                            FormatsCache formatsCache,
                                            BoundaryValueResolver resolver) {
        return newEraDateFormatter(config, new DateDelegate(formatsCache, this), resolver);
    }

    public @Nullable Formatter newGeoWrapperFormatter(GridColumn column, ObjectFormatter formatter) {
        return null; // overridden by the creator of a data source with geometry values
    }

    public Formatter newEraDateFormatter(@Nullable ObjectFormatterConfig config,
                                         DateAndTimeFormatterDelegate<?, ?> delegate,
                                         BoundaryValueResolver resolver) {
        Formatter formatter = newCustomDateTimeFormat(config, DataGridSettings::getEffectiveDatePattern, delegate);
        if (formatter != null) {
            return formatter;
        }

        Builders builders = new Builders();
        appendDate(builders);
        String pattern = builders.sb.toString();
        DateAndTimeFormatter<?, ?> regular = new DateAndTimeFormatter<>(pattern, toFormatter(builders.fb), delegate, resolver, null);
        builders.fb.appendPattern(ERA_PATTERN);
        DateAndTimeFormatter<?, ?> era = new DateAndTimeFormatter<>(pattern, toFormatter(builders.fb), delegate, resolver, null);
        return new EraDateAndTimeFormatter(regular, era);
    }

    protected Formatter newDateFormatter(@Nullable GridColumn column, @Nullable ObjectFormatterConfig config, FormatsCache formatsCache) {
        DateDelegate delegate = new DateDelegate(formatsCache, this);

        Formatter formatter = newCustomDateTimeFormat(config, DataGridSettings::getEffectiveDatePattern, delegate);
        if (formatter != null) {
            return formatter;
        }

        Builders builders = new Builders();
        appendDate(builders);
        formatter = new DateAndTimeFormatter<>(builders.sb.toString(), toFormatter(builders.fb), delegate);
        return checkInfinity(formatter, resolver(column));
    }

    protected Formatter newDateFormatWithTime(@Nullable GridColumn column,
                                              @Nullable ObjectFormatterConfig config,
                                              FormatsCache formatsCache) {
        Builders builders = new Builders();
        appendDate(builders);
        builders.sb.append('T');
        builders.fb.appendLiteral('T');
        appendTime(builders);
        appendFraction(builders, 3);
        builders.sb.append('Z');
        builders.fb.appendLiteral('Z');
        DateAndTimeFormatter<Date, LocalDateTime> formatter = new DateAndTimeFormatter<>(builders.sb.toString(),
            toFormatter(builders.fb), new DateToLocalDateTimeDelegate(formatsCache, this, config));
        return checkInfinity(formatter, resolver(column));
    }

    public <T, V extends TemporalAccessor> CompositeFormatter newZonedTimeFormat(DateAndTimeFormatterDelegate<T, V> delegate,
                                                                                 int scale,
                                                                                 @Nullable ObjectFormatterConfig config) {
        Builders builders = new Builders();
        appendTime(builders);
        appendFraction(builders, scale);
        List<DateTimeFormatter> formatters = appendTimeZone(builders, config);
        String pattern = builders.sb.toString();
        List<Formatter> mapped = ContainerUtil.map(formatters, f -> new DateAndTimeFormatter<>(pattern, f, delegate));
        return new CompositeFormatter(LocalizeValue.localizeTODO("Unexpected data format"), mapped);
    }

    protected static Formatter newShortZonedTimestampFormat(int scale,
                                                            BiFunction<DateTimeFormatter, String, Formatter> mapper,
                                                            @Nullable ObjectFormatterConfig config) {
        Builders builders = createBuildersForTimestamp(scale);
        List<DateTimeFormatter> formatters = appendTimeZone(builders, config);
        String pattern = builders.sb.toString();
        return new CompositeFormatter(LocalizeValue.localizeTODO("Unexpected data format"),
            ContainerUtil.map(formatters, formatter -> mapper.apply(formatter, pattern))
        );
    }

    private static void appendTime(Builders builders) {
        builders.sb.append("HH:mm:ss");
        appendTime(builders.fb);
    }

    public static DateTimeFormatterBuilder appendTime(DateTimeFormatterBuilder builder) {
        return builder.parseLenient()
            .optionalStart().appendValue(ChronoField.HOUR_OF_DAY, 2).optionalEnd()
            .optionalStart().appendLiteral(":").optionalEnd()
            .optionalStart().appendValue(ChronoField.MINUTE_OF_HOUR, 2).optionalEnd()
            .optionalStart().appendLiteral(":").optionalEnd()
            .optionalStart().appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
            .parseStrict();
    }

    private static List<DateTimeFormatter> appendTimeZone(Builders builders, @Nullable ObjectFormatterConfig config) {
        String hourPattern = config != null && config.getMode() == ObjectFormatterMode.NORMALIZE ? "H" : "HH";
        return withZoneOffsets(
            builders,
            builder -> builder.appendOffset("+" + hourPattern + ":MM:ss", "+00:00"),
            builder -> builder.appendOffset("+" + hourPattern + ":mm", "+00:00"),
            builder -> builder.appendOffset("+HHmm", "+0000"),
            builder -> builder.appendZoneOrOffsetId()
        );
    }

    @SafeVarargs
    private static List<DateTimeFormatter> withZoneOffsets(Builders builders, Consumer<DateTimeFormatterBuilder>... consumers) {
        List<DateTimeFormatter> formatters = new ArrayList<>(ContainerUtil.map(consumers, c -> {
            DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .append(toFormatter(builders.fb))
                .appendPattern("[ ]");
            c.accept(builder);
            return toFormatter(builder);
        }));
        formatters.add(toFormatter(builders.fb));
        return formatters;
    }

    private static void appendDate(Builders builders) {
        builders.sb.append(DATE_PATTERN);
        appendDate(builders.fb);
    }

    public static DateTimeFormatterBuilder appendDate(DateTimeFormatterBuilder builder) {
        return builder.parseLenient()
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 7, SignStyle.NEVER)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .parseStrict();
    }

    private static void appendFraction(Builders builders, int scale) {
        builders.sb.append(scale > 0 ? "." : "");
        for (int i = 0; i < scale; i++) {
            builders.sb.append('f');
        }
        if (scale > 0) {
            builders.fb.parseLenient().appendFraction(ChronoField.NANO_OF_SECOND, scale, scale, true).parseStrict();
        }
    }

    protected <T, V extends TemporalAccessor> EraDateAndTimeFormatter eraFormatter(String regular,
                                                                                   DateTimeFormatter regularFormatter,
                                                                                   DateAndTimeFormatterDelegate<T, V> delegate,
                                                                                   BoundaryValueResolver resolver,
                                                                                   boolean omitEmptyTime) {
        DateTimeFormatter era = toFormatter(new DateTimeFormatterBuilder().append(regularFormatter).appendPattern(ERA_PATTERN));
        Builders dateBuilders = omitEmptyTime ? new Builders() : null;
        if (dateBuilders != null) {
            appendDate(dateBuilders);
        }
        DateTimeFormatter dateFormatter = dateBuilders == null ? null : toFormatter(dateBuilders.fb);
        return new EraDateAndTimeFormatter(
            new DateAndTimeFormatter<>(regular, regularFormatter, delegate, resolver, dateFormatter),
            new DateAndTimeFormatter<>(regular, era, delegate, resolver, dateFormatter)
        );
    }

    public static DateTimeFormatter toFormatter(DateTimeFormatterBuilder builder) {
        return builder.toFormatter(Locale.US);
    }

    private static Builders createBuildersForTimestamp(int scale) {
        Builders builders = new Builders();
        appendDate(builders);
        builders.sb.append(' ');
        builders.fb.appendPattern("[ ]");
        appendTime(builders);
        appendFraction(builders, scale);
        return builders;
    }

    private static NumberFormatter configureDecimalFormat(DecimalFormat format, @Nullable ObjectFormatterConfig config) {
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        DataGridSettings settings = DataGridObjectFormatterConfig.getSettings(config);
        String pattern = settings != null ? settings.getEffectiveNumberPattern() : null;
        if (settings != null && config != null && config.supportsNumberFormats()) {
            format.setGroupingUsed(settings.isNumberGroupingEnabled());
            symbols.setGroupingSeparator(settings.getNumberGroupingSeparator());
            symbols.setDecimalSeparator(settings.getDecimalSeparator());
            symbols.setNaN(settings.getNan());
            symbols.setInfinity(settings.getInfinity());
        }
        else {
            format.setGroupingUsed(false); // no thousand separator
            symbols.setDecimalSeparator('.'); // no ',' instead of '.'
            symbols.setNaN("NaN");
            symbols.setInfinity("Infinity");
        }
        symbols.setMinusSign('-');
        format.setDecimalFormatSymbols(symbols);
        if (pattern != null) {
            format.applyLocalizedPattern(pattern);
        }
        return new NumberFormatter(format);
    }

    private static final class Builders {
        private final StringBuilder sb;
        private final DateTimeFormatterBuilder fb;

        private Builders() {
            sb = new StringBuilder();
            fb = new DateTimeFormatterBuilder();
        }
    }

    private static final Key<FormatsCache> FORMATTER_KEY_FORMATS_CACHE_KEY = Key.create("FORMATTER_KEY_FORMATS_CACHE_KEY");
    private static final Key<Integer> FORMATTER_KEY_INT_KEY = Key.create("FORMATTER_KEY_INT_KEY");
    // the priority type of DECIMAL_WITH_PRIORITY_TYPE
    private static final Key<GridTypeKind> FORMATTER_KEY_TYPE_KIND_KEY = Key.create("FORMATTER_KEY_TYPE_KIND_KEY");
    private static final Key<GridColumn> FORMATTER_KEY_GRID_COLUMN_KEY = Key.create("FORMATTER_KEY_GRID_COLUMN_KEY");
    private static final Key<ObjectFormatterConfig> FORMATTER_KEY_CONFIG_KEY = Key.create("FORMATTER_KEY_CONFIG_KEY");

    public static class FormatterKey<T> extends UserDataHolderBase {
        private final String myName;

        public FormatterKey(String name) {
            myName = name;
        }

        public String getName() {
            return myName;
        }

        public boolean is(FormatterKey<?> key) {
            return myName.equals(key.myName);
        }
    }

    private static <T> FormatterKey<T> add(FormatterKey<T> key, @Nullable GridColumn column) {
        key.putUserData(FORMATTER_KEY_GRID_COLUMN_KEY, column);
        return key;
    }

    private static <T> FormatterKey<T> add(FormatterKey<T> key, @Nullable ObjectFormatterConfig config) {
        key.putUserData(FORMATTER_KEY_CONFIG_KEY, config);
        return key;
    }

    private static <T> FormatterKey<T> add(FormatterKey<T> key, int value) {
        key.putUserData(FORMATTER_KEY_INT_KEY, value);
        return key;
    }

    private static <T> FormatterKey<T> add(FormatterKey<T> key, GridTypeKind value) {
        key.putUserData(FORMATTER_KEY_TYPE_KIND_KEY, value);
        return key;
    }

    private static <T> FormatterKey<T> add(FormatterKey<T> key, FormatsCache formatsCache) {
        key.putUserData(FORMATTER_KEY_FORMATS_CACHE_KEY, formatsCache);
        return key;
    }

    private static Formatter checkInfinity(Formatter format, BoundaryValueResolver resolver) {
        return new Formatter() {
            @Override
            public Object parse(String value) throws ParseException {
                return format.parse(value);
            }

            @Override
            public @Nullable Object parse(String value, ParsePosition position) {
                return format.parse(value, position);
            }

            @Override
            public String format(Object value) {
                String infinityString = resolver.getInfinityString(value);
                return infinityString != null ? infinityString : format.format(value);
            }

            @Override
            public String toString() {
                return format.toString();
            }
        };
    }
}
