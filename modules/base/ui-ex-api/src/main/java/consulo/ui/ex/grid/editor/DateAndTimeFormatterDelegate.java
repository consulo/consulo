// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

public abstract class DateAndTimeFormatterDelegate<T, V extends TemporalAccessor> {
    private final TemporalQuery<V>[] myQueries;

    @SafeVarargs
    protected DateAndTimeFormatterDelegate(TemporalQuery<V>... queries) {
        myQueries = queries;
    }

    TemporalQuery<V>[] getQueries() {
        return myQueries;
    }

    protected abstract V toTemporalAccessor(Object value);

    protected abstract T createFromTemporal(V value);
}
