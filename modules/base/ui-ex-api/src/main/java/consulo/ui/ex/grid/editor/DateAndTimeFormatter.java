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
import consulo.ui.grid.editor.BoundaryValueResolver;
import org.jspecify.annotations.Nullable;

import java.text.ParsePosition;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

public class DateAndTimeFormatter<T, V extends TemporalAccessor> extends FormatterImpl {
    private final DateTimeFormatter myFormatter;
    private final String myPattern;
    private final DateAndTimeFormatterDelegate<T, V> myDelegate;
    private final BoundaryValueResolver myBoundaryValuesResolver;
    private final @Nullable DateTimeFormatter myDateFormatterForValueWithoutTime;

    public DateAndTimeFormatter(String pattern, DateTimeFormatter formatter, DateAndTimeFormatterDelegate<T, V> delegate) {
        this(pattern, formatter, delegate, BoundaryValueResolver.ALWAYS_NULL, null);
    }

    public DateAndTimeFormatter(String pattern,
                                DateTimeFormatter formatter,
                                DateAndTimeFormatterDelegate<T, V> delegate,
                                BoundaryValueResolver resolver,
                                @Nullable DateTimeFormatter dateFormatterForValueWithoutTime) {
        myPattern = pattern;
        myFormatter = formatter;
        myDelegate = delegate;
        myBoundaryValuesResolver = resolver;
        myDateFormatterForValueWithoutTime = dateFormatterForValueWithoutTime;
    }

    @Override
    protected LocalizeValue getErrorMessage() {
        return LocalizeValue.localizeTODO("Unexpected data format");
    }

    private @Nullable V query(@Nullable TemporalAccessor parsed, TemporalQuery<V>[] queries) {
        if (parsed == null) {
            return null;
        }
        for (TemporalQuery<V> query : queries) {
            try {
                return parsed.query(query);
            }
            catch (RuntimeException ignore) {
            }
        }
        return null;
    }

    private @Nullable TemporalAccessor parseValue(String value, ParsePosition position) {
        try {
            TemporalAccessor parsed = myFormatter.parse(value, position);
            if (position.getIndex() != value.length() || position.getErrorIndex() != -1) {
                return null;
            }
            return parsed;
        }
        catch (DateTimeParseException e) {
            position.setErrorIndex(e.getErrorIndex());
            return null;
        }
    }

    @Override
    public @Nullable Object parse(String value, ParsePosition position) {
        Object boundary = myBoundaryValuesResolver.createFromInfinityString(value);
        if (boundary != null) {
            return boundary;
        }
        TemporalAccessor parsed = parseValue(value, position);
        if (parsed == null) {
            return null;
        }
        V temporal = query(parsed, myDelegate.getQueries());
        if (temporal == null) {
            position.setErrorIndex(0);
            return null;
        }
        T result = myDelegate.createFromTemporal(temporal);
        String boundaryString = myBoundaryValuesResolver.resolve(result);
        return boundaryString != null ? myBoundaryValuesResolver.createFromInfinityString(boundaryString) : result;
    }

    @Override
    public String format(Object value) {
        String boundary = myBoundaryValuesResolver.resolve(value);
        if (boundary != null) {
            return boundary;
        }
        V temporalAccessor = myDelegate.toTemporalAccessor(value);
        if (myDateFormatterForValueWithoutTime != null && DataGridFormattersUtilCore.isEmptyTime(temporalAccessor)) {
            return myDateFormatterForValueWithoutTime.format(temporalAccessor);
        }
        return myFormatter.format(temporalAccessor);
    }

    public TemporalAccessor getTemporalAccessor(Object value) {
        return myDelegate.toTemporalAccessor(value);
    }

    @Override
    public String toString() {
        return myPattern;
    }
}
