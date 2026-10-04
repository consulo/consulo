// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.LazyValue;

import java.util.List;

public interface CsvFormats {
    LazyValue<CsvFormat> TSV_FORMAT = LazyValue.notNull(() -> {
        return xsvFormat(LocalizeValue.localizeTODO("TSV").get(), "Tab-separated (TSV)_id", "\t");
    });
    LazyValue<CsvFormat> CSV_FORMAT = LazyValue.notNull(() -> {
        return xsvFormat(LocalizeValue.localizeTODO("CSV").get(), "Comma-separated (CSV)_id", ",");
    });
    LazyValue<CsvFormat> PIPE_SEPARATED_FORMAT = LazyValue.notNull(() -> {
        return xsvFormat(LocalizeValue.localizeTODO("Pipe-separated").get(), "Pipe-separated_id", "|");
    });
    LazyValue<CsvFormat> SEMICOLON_SEPARATED_FORMAT = LazyValue.notNull(() -> {
        return xsvFormat(LocalizeValue.localizeTODO("Semicolon-separated").get(), "Semicolon-separated_id", ";");
    });

    private static CsvFormat xsvFormat(String name, String id, String valueSeparator) {
        List<CsvRecordFormat.Quotes> quotes = List.of(new CsvRecordFormat.Quotes("\"", "\"", "\"\"", "\"\""),
                                                      new CsvRecordFormat.Quotes("'", "'", "''", "''"));
        CsvRecordFormat.QuotationPolicy quotationPolicy = CsvRecordFormat.QuotationPolicy.AS_NEEDED;
        CsvRecordFormat dataFormat = new CsvRecordFormat("", "", "", quotes, quotationPolicy, valueSeparator, "\n", false);
        return new CsvFormat(name, dataFormat, null, id, false);
    }
}
