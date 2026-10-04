// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.document.util.TextRange;

import java.util.List;

public class CsvRecord {
    public final TextRange range;
    public final List<ValueRange> values;
    public final boolean hasRecordSeparator;

    public CsvRecord(TextRange range,
                     List<ValueRange> values,
                     boolean hasRecordSeparator) {
        this.range = range;
        this.values = values;
        this.hasRecordSeparator = hasRecordSeparator;
    }
}
