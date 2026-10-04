// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import org.jspecify.annotations.Nullable;

import java.util.List;

public class CsvParserResult {
    private final CsvFormat myFormat;
    private final CharSequence mySequence;
    private final List<CsvRecord> myRecords;
    private final @Nullable CsvRecord myHeader;
    private final int myColumnsCount;

    public CsvParserResult(CsvFormat format,
                           CharSequence sequence,
                           List<CsvRecord> records,
                           @Nullable CsvRecord header,
                           int columnsCount) {
        myFormat = format;
        mySequence = sequence;
        myRecords = records;
        myHeader = header;
        myColumnsCount = columnsCount;
    }

    public List<CsvRecord> getRecords() {
        return myRecords;
    }

    public @Nullable CsvRecord getHeader() {
        return myHeader;
    }

    public CsvFormat getFormat() {
        return myFormat;
    }

    public CharSequence getSequence() {
        return mySequence;
    }

    public int getColumnsCount() {
        return myColumnsCount;
    }
}
