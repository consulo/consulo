// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class CsvFormat {
    public final String name;
    public final CsvRecordFormat dataRecord;
    public final @Nullable CsvRecordFormat headerRecord;
    public final boolean rowNumbers;
    public final String id;

    public CsvFormat(CsvRecordFormat dataRecord, @Nullable CsvRecordFormat headerRecord, boolean rowNumbers) {
        this("", dataRecord, headerRecord, rowNumbers);
    }

    public CsvFormat(String name, CsvRecordFormat dataRecord, @Nullable CsvRecordFormat headerRecord, boolean rowNumbers) {
        this(name, dataRecord, headerRecord, UUID.randomUUID().toString(), rowNumbers);
    }

    public CsvFormat(String name,
                     CsvRecordFormat dataRecord,
                     @Nullable CsvRecordFormat headerRecord,
                     String id,
                     boolean rowNumbers) {
        this.name = name;
        this.dataRecord = dataRecord;
        this.headerRecord = headerRecord;
        this.rowNumbers = rowNumbers;
        this.id = id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CsvFormat format)) {
            return false;
        }

        if (rowNumbers != format.rowNumbers) {
            return false;
        }
        if (!name.equals(format.name)) {
            return false;
        }
        if (!dataRecord.equals(format.dataRecord)) {
            return false;
        }
        if (headerRecord != null ? !headerRecord.equals(format.headerRecord) : format.headerRecord != null) {
            return false;
        }
        if (!id.equals(format.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + dataRecord.hashCode();
        result = 31 * result + (headerRecord != null ? headerRecord.hashCode() : 0);
        result = 31 * result + (rowNumbers ? 1 : 0);
        result = 31 * result + id.hashCode();
        return result;
    }

    public static int indexOfFormatNamed(List<CsvFormat> formats, @Nullable String name) {
        return name == null ? -1 : ContainerUtil.indexOf(formats, format -> StringUtil.equals(name, format.name));
    }
}
