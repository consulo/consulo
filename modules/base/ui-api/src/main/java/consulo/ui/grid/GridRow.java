// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.collection.JBIterator;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;

/**
 * @author Liudmila Kornilova
 **/
public interface GridRow extends Iterable<@Nullable Object> {
    @Nullable
    Object getValue(int columnNum);

    int getSize();

    void setValue(int i, @Nullable Object object);

    @Override
    default Iterator<@Nullable Object> iterator() {
        return new JBIterator<>() {
            private int myNextValueIdx;

            @Override
            protected @Nullable Object nextImpl() {
                return myNextValueIdx < getSize() ? getValue(myNextValueIdx++) : stop();
            }
        };
    }

    static @Nullable Object[] getValues(GridRow row) {
        @Nullable Object[] v = new @Nullable Object[row.getSize()];
        for (int i = 0; i < row.getSize(); i++) {
            v[i] = row.getValue(i);
        }
        return v;
    }

    int getRowNum();

    static int toRealIdx(GridRow row) {
        return row.getRowNum() - 1;
    }

    static boolean equals(@Nullable GridRow row1, @Nullable GridRow row2) {
        if (row1 == row2) {
            return true;
        }
        if (row1 == null || row2 == null) {
            return false;
        }
        if (row1.getRowNum() != row2.getRowNum()) {
            return false;
        }
        if (row1.getSize() != row2.getSize()) {
            return false;
        }
        for (int i = 0; i < row1.getSize(); i++) {
            if (!Objects.equals(row1.getValue(i), row2.getValue(i))) {
                return false;
            }
        }
        return true;
    }
}
