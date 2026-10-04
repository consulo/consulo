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

/**
 * What a data source allows a grid to do with its columns. The members which need languages or code fragments are left out: they
 * are not visible from {@code consulo.ui.api}.
 * <p/>
 * Every member has the answer of a data source which places no limits, so an implementation overrides only what it limits. A grid
 * finds its helper with {@link GridHelper#get(CoreGrid)}.
 */
public interface CoreGridHelper {
    /**
     * Whether columns can be added, cloned, renamed, moved and deleted. The default allows it while the grid is editable and ready,
     * its data source has a {@link GridMutator.ColumnsMutator} and it holds at least one row.
     */
    default boolean canMutateColumns(CoreGrid<GridRow, GridColumn> grid) {
        return grid.isEditable()
            && grid.isReady()
            && grid.getDataHookup().getMutator() instanceof GridMutator.ColumnsMutator
            && grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getRowCount() != 0;
    }

    /**
     * Whether a change of a column - a clone, a delete - applies to every record of a collection, rather than to the rows shown.
     * The column actions which cannot do that hide themselves.
     */
    default boolean isModifyColumnAcrossCollection() {
        return false;
    }

    default boolean isSortingApplicable() {
        return true;
    }

    default boolean isSortingApplicable(ModelIndex<GridColumn> colIdx) {
        return isSortingApplicable();
    }
}
