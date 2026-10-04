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
package consulo.ui.ex.grid;

import consulo.ui.grid.DataGrid;
import consulo.util.dataholder.Key;

/**
 * The data context keys of the data grid. The keys of the database settings, run layout and content are left out.
 *
 * @author Gregory.Shrago
 */
public final class DatabaseDataKeys {
    /**
     * The grid the data context belongs to: a grid created with {@link GridUtil#createDataGrid} answers it for itself, so the grid
     * actions find it when they are run from its context menu or with a shortcut while it has the focus.
     */
    public static final Key<DataGrid> DATA_GRID_KEY = Key.create("DATA_GRID_KEY");

    private DatabaseDataKeys() {
    }
}
