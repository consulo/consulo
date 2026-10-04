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

/**
 * A request with a fixed value: everything else is answered by the request it wraps.
 */
final class FixedValueGridCellRequest<Row, Column> implements FixedGridCellRequest<Row, Column> {
    private final GridCellRequest<Row, Column> myDelegate;
    private final @Nullable Object myValueOverride;

    FixedValueGridCellRequest(GridCellRequest<Row, Column> delegate, @Nullable Object valueOverride) {
        myDelegate = delegate;
        myValueOverride = valueOverride;
    }

    @Override
    public CoreGrid<Row, Column> getGrid() {
        return myDelegate.getGrid();
    }

    @Override
    public ModelIndex<Row> getRowIdx() {
        return myDelegate.getRowIdx();
    }

    @Override
    public ModelIndex<Column> getColumnIdx() {
        return myDelegate.getColumnIdx();
    }

    @Override
    public @Nullable Column getColumn() {
        return myDelegate.getColumn();
    }

    @Override
    public @Nullable Object getValue() {
        return myValueOverride;
    }

    @Override
    public boolean isValid() {
        return myDelegate.isValid();
    }

    @Override
    public boolean isColumnIdxValid() {
        return myDelegate.isColumnIdxValid();
    }

    @Override
    public boolean isRowIdxValid() {
        return myDelegate.isRowIdxValid();
    }
}
