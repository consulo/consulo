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

import java.util.function.Supplier;

/**
 * The place of a request which moves a column in the data source, after the user moved it in the view. The grid adjusts its
 * columns - widths, selection, the order of the view - through {@link #adjustColumnsUI()} once the data source has the new order.
 */
public final class MoveColumnsRequestPlace extends LongActionRequestPlace {
    private final Runnable myAdjustColumnsUI;

    public MoveColumnsRequestPlace(CoreGrid<GridRow, GridColumn> grid, Supplier<AutoCloseable> loadingUI, Runnable adjustColumnsUI) {
        super(grid, loadingUI);
        myAdjustColumnsUI = adjustColumnsUI;
    }

    public void adjustColumnsUI() {
        myAdjustColumnsUI.run();
    }
}
