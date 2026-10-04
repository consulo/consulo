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

import consulo.application.Application;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.ex.grid.DataGridSettingsListener;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ObjectFormatterConfig;
import consulo.util.dataholder.NotNullLazyKey;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static consulo.ui.ex.grid.editor.FormatterCreator.FormatterKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.LOCAL_DATE_FORMATTER_KEY;
import static consulo.ui.ex.grid.editor.FormatterCreator.LOCAL_DATE_WITH_MILLI_FORMATTER_KEY;
import static consulo.ui.ex.grid.editor.FormatterCreator.OFFSET_DATE_TIME_FORMATTER_KEY;
import static consulo.ui.ex.grid.editor.FormatterCreator.SHORT_TIMESTAMP_FORMATTER_KEY;
import static consulo.ui.ex.grid.editor.FormatterCreator.SIMPLE_DATE_FORMATTER_KEY;
import static consulo.ui.ex.grid.editor.FormatterCreator.SIMPLE_TIMESTAMP_FORMATTER_KEY;
import static consulo.ui.ex.grid.editor.FormatterCreator.TIMESTAMP_WITH_MILLI_FORMATTER_KEY;
import static consulo.ui.ex.grid.editor.FormatterCreator.getBigIntKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getDateKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getDecimalKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getDecimalWithPriorityTypeKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getDoubleKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getFloatKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getIntKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getLongKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getShortEraZonedTimestampKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getTimeKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getTimestampKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getZonedTimeKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getZonedTimestampKey;

/**
 * SimpleDateFormat is not thread-safe therefore FormatsCache is not thread-safe too
 * <p/>
 * Notes:
 * <ul>
 * <li>the cache is cleared on {@link DataGridSettingsListener};</li>
 * <li>{@link #getBigDecimalWithPriorityTypeFormatProvider} takes the {@link GridTypeKind} of the column;</li>
 * <li>a {@link Triple} is a key of the cache.</li>
 * </ul>
 */
public class FormatsCache {
    // TODO: Get rid of SimpleDateFormat
    // Until then all users should synchronize on the same lock (currently DataGridFormattersUtilCore.class)
    public static final FormatProvider<String, SimpleDateFormat> SIMPLE_TIMESTAMP_FORMAT_PROVIDER =
        new BaseFormatProvider<>("SIMPLE_TIMESTAMP", SIMPLE_TIMESTAMP_FORMATTER_KEY);
    public static final FormatProvider<String, SimpleDateFormat> SHORT_TIMESTAMP_FORMAT_PROVIDER =
        new BaseFormatProvider<>("SHORT_TIMESTAMP", SHORT_TIMESTAMP_FORMATTER_KEY);
    public static final FormatProvider<String, SimpleDateFormat> TIMESTAMP_WITH_MILLI_FORMAT_PROVIDER =
        new BaseFormatProvider<>("TIMESTAMP_WITH_MILLI", TIMESTAMP_WITH_MILLI_FORMATTER_KEY);
    public static final FormatProvider<String, SimpleDateFormat> SIMPLE_DATE_FORMAT_PROVIDER =
        new BaseFormatProvider<>("SIMPLE_DATE", SIMPLE_DATE_FORMATTER_KEY);

    public static final FormatProvider<String, DateTimeFormatter> LOCAL_DATE_WITH_MILLI_FORMAT_PROVIDER =
        new BaseFormatProvider<>("LOCAL_DATE_WITH_MILLI", LOCAL_DATE_WITH_MILLI_FORMATTER_KEY);
    public static final FormatProvider<String, DateTimeFormatter> LOCAL_DATE_FORMAT_PROVIDER =
        new BaseFormatProvider<>("LOCAL_DATE", LOCAL_DATE_FORMATTER_KEY);
    public static final FormatProvider<String, DateTimeFormatter> OFFSET_DATE_TIME_FORMAT_PROVIDER =
        new BaseFormatProvider<>("OFFSET_DATE_TIME", OFFSET_DATE_TIME_FORMATTER_KEY);

    private static final Logger LOG = Logger.getInstance(FormatsCache.class);
    private static final NotNullLazyKey<FormatsCache, CoreGrid<?, ?>> FORMATS_CACHE_KEY =
        NotNullLazyKey.create("FORMATS_CACHE_KEY", FormatsCache::createCache);

    private final Map<Object, Object> myCache = new ConcurrentHashMap<>();

    private static FormatsCache createCache(CoreGrid<?, ?> grid) {
        FormatsCache cache = new FormatsCache();
        if (!Disposer.isDisposed(grid)) {
            Application.get().getMessageBus().connect(grid).subscribe(DataGridSettingsListener.class, () -> {
                cache.myCache.clear();
            });
        }
        return cache;
    }

    public static FormatsCache get(CoreGrid<?, ?> grid) {
        return FORMATS_CACHE_KEY.getValue(grid);
    }

    public <K, T> T get(FormatProvider<K, T> provider, FormatterCreator formatterCreator) {
        return get(provider, formatterCreator, true);
    }

    @SuppressWarnings("unchecked")
    public <K, T> T get(FormatProvider<K, T> provider, FormatterCreator formatterCreator, boolean cache) {
        K key = provider.getCacheKey();
        if (cache) {
            Object res = myCache.get(key);
            if (res != null) {
                return (T) res;
            }
        }
        T r = provider.createFormatter(this, formatterCreator);
        if (cache) {
            myCache.put(key, r);
        }
        int cacheSize = myCache.size();
        if (cacheSize > 0 &&
            (cacheSize % 10_000 == 0 && cacheSize < 100_000 ||
                cacheSize % 100_000 == 0)) {
            LOG.error("Formats cash size seems to be too big: " + cacheSize);
        }
        return r;
    }

    public static FormatProvider<Pair<String, @Nullable ObjectFormatterConfig>, NumberFormatter> getIntFormatProvider(
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Pair<>("INT", config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getIntKey(config))
        );
    }

    public static FormatProvider<Pair<String, @Nullable ObjectFormatterConfig>, NumberFormatter> getLongFormatProvider(
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Pair<>("LONG", config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getLongKey(config))
        );
    }

    public static FormatProvider<Pair<String, @Nullable ObjectFormatterConfig>, NumberFormatter> getBigIntFormatProvider(
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Pair<>("BIG_INT", config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getBigIntKey(config))
        );
    }

    public static FormatProvider<Pair<String, @Nullable ObjectFormatterConfig>, NumberFormatter> getFloatFormatProvider(
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Pair<>("FLOAT", config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getFloatKey(config))
        );
    }

    public static FormatProvider<Pair<String, @Nullable ObjectFormatterConfig>, NumberFormatter> getDoubleFormatProvider(
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Pair<>("DOUBLE", config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getDoubleKey(config))
        );
    }

    public static FormatProvider<Triple<String, @Nullable GridColumn, @Nullable ObjectFormatterConfig>, Formatter>
    getZonedTimeFormatProvider(
        @Nullable GridColumn column,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("ZONED_TIME", column, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getZonedTimeKey(column, config, formatsCache))
        );
    }

    public static FormatProvider<Triple<String, @Nullable GridColumn, @Nullable ObjectFormatterConfig>, CompositeFormatter>
    getZonedTimestampFormatProvider(
        @Nullable GridColumn column,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("ZONED_TIMESTAMP", column, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getZonedTimestampKey(column, config, formatsCache))
        );
    }

    public static FormatProvider<Triple<String, @Nullable GridColumn, @Nullable ObjectFormatterConfig>, Formatter>
    getShortEraZonedTimestampFormatProvider(
        @Nullable GridColumn column,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("SHORT_ERA_ZONED_TIMESTAMP", column, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getShortEraZonedTimestampKey(column, config, formatsCache))
        );
    }

    /**
     * @param type the kind of the column, which decides the class a number parses to
     */
    public static FormatProvider<Triple<String, GridTypeKind, @Nullable ObjectFormatterConfig>, NumberFormatter>
    getBigDecimalWithPriorityTypeFormatProvider(
        GridTypeKind type,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("DECIMAL_WITH_PRIORITY_TYPE", type, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getDecimalWithPriorityTypeKey(type, config))
        );
    }

    public static FormatProvider<Triple<String, @Nullable GridColumn, @Nullable ObjectFormatterConfig>, Formatter> getDateFormatProvider(
        @Nullable GridColumn column,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("DATE", column, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getDateKey(column, config, formatsCache))
        );
    }

    public static FormatProvider<Triple<String, @Nullable GridColumn, @Nullable ObjectFormatterConfig>, NumberFormatter>
    getDecimalFormatProvider(
        @Nullable GridColumn column,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("DECIMAL", column, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getDecimalKey(column, config))
        );
    }

    public static FormatProvider<Triple<String, @Nullable GridColumn, @Nullable ObjectFormatterConfig>, Formatter> getTimeFormatProvider(
        @Nullable GridColumn column,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("TIME", column, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getTimeKey(column, config, formatsCache))
        );
    }

    public static FormatProvider<Triple<String, @Nullable GridColumn, @Nullable ObjectFormatterConfig>, Formatter>
    getTimestampFormatProvider(
        @Nullable GridColumn column,
        @Nullable ObjectFormatterConfig config
    ) {
        return new BaseFormatProvider<>(
            new Triple<>("TIMESTAMP", column, config),
            (formatsCache, formatterCreator) -> formatterCreator.create(getTimestampKey(column, config, formatsCache))
        );
    }

    public interface FormatProvider<K, T> {
        K getCacheKey();

        T createFormatter(FormatsCache formatsCache, FormatterCreator formatterCreator);
    }

    public static class BaseFormatProvider<K, T> implements FormatProvider<K, T> {
        private final K myKey;
        private final BiFunction<FormatsCache, FormatterCreator, T> myCompute;

        BaseFormatProvider(K key, FormatterKey<T> formatterKey) {
            myKey = key;
            myCompute = (formatsCache, formatterCreator) -> formatterCreator.create(formatterKey);
        }

        public BaseFormatProvider(K key, BiFunction<FormatsCache, FormatterCreator, T> compute) {
            myKey = key;
            myCompute = compute;
        }

        @Override
        public K getCacheKey() {
            return myKey;
        }

        @Override
        public T createFormatter(FormatsCache formatsCache, FormatterCreator formatterCreator) {
            return myCompute.apply(formatsCache, formatterCreator);
        }
    }

    /**
     * Three values, a key of the cache.
     */
    public record Triple<A extends @Nullable Object, B extends @Nullable Object, C extends @Nullable Object>(A first, B second, C third) {
    }
}
