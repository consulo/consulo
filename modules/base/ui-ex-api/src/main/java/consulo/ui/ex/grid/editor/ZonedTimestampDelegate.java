// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

public abstract class ZonedTimestampDelegate<T> extends DateAndTimeFormatterDelegate<T, OffsetDateTime> {
    public ZonedTimestampDelegate() {
        super(OffsetDateTime::from,
            temporal -> ZonedDateTime.from(temporal).toOffsetDateTime(),
            temporal -> LocalDateTime.from(temporal).atOffset(DataGridFormattersUtilCore.getDefaultOffset()),
            temporal -> LocalDate.from(temporal).atTime(0, 0, 0).atOffset(DataGridFormattersUtilCore.getDefaultOffset()));
    }
}
