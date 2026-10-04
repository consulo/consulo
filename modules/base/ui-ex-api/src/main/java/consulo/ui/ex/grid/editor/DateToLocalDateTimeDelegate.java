// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import consulo.ui.grid.ObjectFormatterConfig;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Date;

public class DateToLocalDateTimeDelegate extends DateAndTimeFormatterDelegate<Date, LocalDateTime> {
    private final FormatsCache myFormatsCache;
    private final FormatterCreator myFormatterCreator;
    private final @Nullable ObjectFormatterConfig myConfig;

    public DateToLocalDateTimeDelegate(FormatsCache formatsCache,
                                       FormatterCreator formatterCreator,
                                       @Nullable ObjectFormatterConfig config) {
        super(LocalDateTime::from);
        myFormatsCache = formatsCache;
        myFormatterCreator = formatterCreator;
        myConfig = config;
    }

    @Override
    protected Date createFromTemporal(LocalDateTime value) {
        return DataGridFormattersUtilCore.fromLocalDateTime(value, myFormatsCache, myFormatterCreator, myConfig);
    }

    @Override
    protected LocalDateTime toTemporalAccessor(Object value) {
        if (!(value instanceof Date)) {
            throw new IllegalArgumentException("Value must be of type Date");
        }
        return DataGridFormattersUtilCore.fromDateToLocalDateTime((Date) value, myFormatsCache, myFormatterCreator, myConfig);
    }
}
