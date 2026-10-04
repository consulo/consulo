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

import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * What a data source allows a grid to do with its rows, on top of {@link CoreGridHelper}. The members about dumps, extractors,
 * icons, files and PSI are left out.
 * <p/>
 * A data source installs its helper on the grid with {@link #set(CoreGrid, GridHelper)}, usually from the configurator of the grid.
 * A grid without one gets {@link #DEFAULT}, which places no limits.
 */
public interface GridHelper extends CoreGridHelper {
    Key<GridHelper> GRID_HELPER_KEY = Key.create("GRID_HELPER_KEY");

    /**
     * The helper of a grid which has none installed.
     */
    GridHelper DEFAULT = new GridHelper() {
    };

    static GridHelper get(CoreGrid<?, ?> grid) {
        GridHelper helper = GRID_HELPER_KEY.get(grid);
        return helper != null ? helper : DEFAULT;
    }

    static void set(CoreGrid<?, ?> grid, @Nullable GridHelper helper) {
        GRID_HELPER_KEY.set(grid, helper);
    }

    /**
     * Whether a new row can be added. The add row action is visible but disabled when it cannot.
     */
    default boolean canAddRow(CoreGrid<GridRow, GridColumn> grid) {
        return true;
    }

    /**
     * Whether the rows of a grid which is not {@link CoreGrid#isEditable() editable} itself can still be changed, through a data
     * source which edits them somewhere else. The add and delete row actions stay visible when it can.
     */
    default boolean hasTargetForEditing(CoreGrid<GridRow, GridColumn> grid) {
        return true;
    }
}
