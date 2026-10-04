// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

public final class CsvImportUtil {
    public static TypeMerger getPreferredTypeMergerBasedOnContent(Iterable<@Nullable String> values,
                                                                  TypeMerger stringMerger,
                                                                  TypeMerger... mergers) {
        @Nullable TypeMerger merger = null;
        for (@Nullable String value : values) {
            if (value == null) {
                continue;
            }
            TypeMerger nextMerger = ObjectUtil.notNull(getType(value, mergers), stringMerger);
            merger = merger == null ? nextMerger : merger.merge(nextMerger);
        }
        return merger == null ? stringMerger : merger;
    }

    public static @Nullable TypeMerger getType(String string, TypeMerger[] mergers) {
        for (TypeMerger merger : mergers) {
            if (merger.isSuitable(string)) {
                return merger;
            }
        }
        return null;
    }
}
