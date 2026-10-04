// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import consulo.ui.grid.ObjectFormatterConfig;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static consulo.ui.ex.grid.editor.DataGridFormattersUtilCore.getDefaultOffset;

public class TimestampDelegate extends DateAndTimeFormatterDelegate<Timestamp, OffsetDateTime> {
    private final FormatsCache myFormatsCache;
    private final FormatterCreator myFormatterCreator;
    private final @Nullable ObjectFormatterConfig myConfig;

    public TimestampDelegate(FormatsCache formatsCache, FormatterCreator creator, @Nullable ObjectFormatterConfig config) {
        super(OffsetDateTime::from,
            temporal -> LocalDateTime.from(temporal).atOffset(getDefaultOffset()),
            temporal -> LocalDate.from(temporal).atStartOfDay().atOffset(getDefaultOffset()));
        myFormatsCache = formatsCache;
        myFormatterCreator = creator;
        myConfig = config;
    }

    @Override
    protected OffsetDateTime toTemporalAccessor(Object value) {
        if (!(value instanceof Timestamp)) {
            throw new IllegalArgumentException("Value must be of type Timestamp");
        }
        return DataGridFormattersUtilCore.fromTimestamp((Timestamp) value, myFormatsCache, myFormatterCreator, myConfig);
    }

    @Override
    protected Timestamp createFromTemporal(OffsetDateTime value) {
        return DataGridFormattersUtilCore.fromOffsetDateTime(value, myFormatsCache, myFormatterCreator, myConfig);
    }
}
