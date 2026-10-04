// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;

/**
 * Text cells of a numeric column are compared as numbers, by the {@link GridTypeKind} of the column. Large object values are
 * not compared by their content.
 */
public class GridRowComparator implements Comparator<GridRow> {
    protected final GridColumn myColumn;

    protected GridRowComparator(GridColumn column) {
        myColumn = column;
    }

    @Override
    public int compare(GridRow row1, GridRow row2) {
        Object v1 = myColumn.getValue(row1);
        Object v2 = myColumn.getValue(row2);
        return compareObjects(v1, v2);
    }

    public int compareObjects(@Nullable Object v1, @Nullable Object v2) {
        if (v1 instanceof String s1 && v2 instanceof String s2) {
            GridTypeKind kind = myColumn.getType().getKind();
            try {
                switch (kind) {
                    case INTEGER:
                        return new BigInteger(s1).compareTo(new BigInteger(s2));
                    case FLOAT:
                        return Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));
                    case DECIMAL:
                        return new BigDecimal(s1).compareTo(new BigDecimal(s2));
                    default:
                        break;
                }
            }
            catch (NumberFormatException ignored) {
            }
        }
        return compareValues(v1, v2);
    }

    public static @Nullable GridRowComparator create(GridColumn column) {
        return GridUtilCore.isRowId(column) ? null : new GridRowComparator(column);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int compareValues(@Nullable Object v1, @Nullable Object v2) {
        // NULLs first
        if (v1 == v2) {
            return 0;
        }
        else if (v1 == null) {
            return -1;
        }
        else if (v2 == null) {
            return 1;
        }

        // Failed to load errors next
        if (GridUtilCore.isFailedToLoad(v1)) {
            return GridUtilCore.isFailedToLoad(v2) ? ((String) v1).compareTo((String) v2) : -1;
        }
        else if (GridUtilCore.isFailedToLoad(v2)) {
            return 1;
        }

        // Generic Comparable case and the rest
        if (v1 instanceof Comparable && v2 instanceof Comparable && v1.getClass() == v2.getClass()) {
            return ((Comparable) v1).compareTo(v2);
        }
        else if (v1 instanceof Number && v2 instanceof Number) {
            // cheap CCE fix for the time being
            int result1 = Double.compare(((Number) v1).doubleValue(), ((Number) v2).doubleValue());
            long result2 = ((Number) v1).longValue() - ((Number) v2).longValue();
            if (result1 < 0 && result2 < 0) {
                return -1;
            }
            if (result1 > 0 && result2 > 0) {
                return 1;
            }
            return 0;
        }
        else if (v1 instanceof Object[] array1 && v2 instanceof Object[] array2) {
            int maxLength = Math.max(array1.length, array2.length);
            for (int i = 0; i < maxLength; i++) {
                int comparisonResult = compareValues(i < array1.length ? array1[i] : null, i < array2.length ? array2[i] : null);
                if (comparisonResult != 0) {
                    return comparisonResult;
                }
            }
            return 0;
        }
        return String.valueOf(v1.getClass().getCanonicalName()).compareTo(String.valueOf(v2.getClass().getCanonicalName()));
    }
}
