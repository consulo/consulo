// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.util.dataholder.Key;

public interface CsvFormatEditor {
    Key<CsvFormatEditor> CSV_FORMAT_EDITOR_KEY = Key.create("CSV_FORMAT_EDITOR_KEY");

    boolean firstRowIsHeader();

    void setFirstRowIsHeader(boolean value);
}
