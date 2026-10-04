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

import java.util.EventListener;

/**
 * Listens to the selection, the content and the edited values of a single grid. Listeners are added on the grid with
 * {@link DataGrid#addDataGridListener}, not through the message bus.
 */
public interface DataGridListener extends EventListener {
    default void onSelectionChanged(DataGrid dataGrid) {
    }

    default void onSelectionChanged(DataGrid dataGrid, boolean isAdjusting) {
        if (!isAdjusting) {
            onSelectionChanged(dataGrid);
        }
    }

    default void onContentChanged(DataGrid dataGrid, GridRequestSource.@Nullable RequestPlace place) {
    }

    default void onValueEdited(DataGrid dataGrid, @Nullable Object object) {
    }
}
