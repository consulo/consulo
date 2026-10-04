// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;

public abstract class ShortZonedTimestampDelegate<T> extends DateAndTimeFormatterDelegate<T, TemporalAccessor> {
    public ShortZonedTimestampDelegate() {
        super(LocalDateTime::from, temporal -> LocalDate.from(temporal).atTime(0, 0, 0));
    }
}
