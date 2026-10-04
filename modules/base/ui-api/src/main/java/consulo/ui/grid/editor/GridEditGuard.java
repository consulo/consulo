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
package consulo.ui.grid.editor;

import consulo.localize.LocalizeValue;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * A reason to reject an edit of a grid, which gives the read-only hint of an editor
 * ({@link GridCellEditorPresentation#readOnlyHint()}); whether the editor is read-only is decided by
 * {@link CoreGrid#isEditable()} and {@link GridCellEditorFactory.IsEditableChecker}.
 */
public interface GridEditGuard {
    LocalizeValue getReasonText(CoreGrid<GridRow, GridColumn> grid);

    boolean rejectEdit(CoreGrid<GridRow, GridColumn> grid);

    static @Nullable GridEditGuard get(CoreGrid<GridRow, GridColumn> grid) {
        Set<GridEditGuard> guards = GridCellEditorHelper.get(grid).getEditGuards();
        if (guards == null) {
            return null;
        }
        for (GridEditGuard guard : guards) {
            if (guard.rejectEdit(grid)) {
                return guard;
            }
        }
        return null;
    }
}
