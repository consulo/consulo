// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import java.time.LocalDate;
import java.util.Date;

public class DateDelegate extends DateAndTimeFormatterDelegate<Date, LocalDate> {
    private final FormatsCache myFormatsCache;
    private final FormatterCreator myFormatterCreator;

    public DateDelegate(FormatsCache formatsCache, FormatterCreator formatterCreator) {
        super(LocalDate::from);
        myFormatsCache = formatsCache;
        myFormatterCreator = formatterCreator;
    }

    @Override
    protected Date createFromTemporal(LocalDate value) {
        return DataGridFormattersUtilCore.fromLocalDate(value, myFormatsCache, myFormatterCreator);
    }

    @Override
    protected LocalDate toTemporalAccessor(Object value) {
        if (!(value instanceof Date)) {
            throw new IllegalArgumentException("Value must be of type Date");
        }
        return DataGridFormattersUtilCore.fromDate((Date) value, myFormatsCache, myFormatterCreator);
    }
}
