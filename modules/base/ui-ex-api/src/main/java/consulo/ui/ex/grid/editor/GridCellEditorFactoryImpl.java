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
package consulo.ui.ex.grid.editor;

import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorFactoryProvider;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * The editor factories of the grid, in order - on equal suitability the earlier factory wins. There is no blob editor: the grid
 * model has no binary values such an editor edits.
 */
public class GridCellEditorFactoryImpl implements GridCellEditorFactoryProvider {
    private static final GridCellEditorFactoryImpl INSTANCE = new GridCellEditorFactoryImpl();

    protected final List<? extends GridCellEditorFactory> myDefaultFactories = createFactories();

    protected List<? extends GridCellEditorFactory> createFactories() {
        return Arrays.asList(new DefaultNumericEditorFactory(), new DefaultDateEditorFactory(),
            new DefaultTimestampEditorFactory(DefaultTimestampEditorFactory.Mode.DATE),
            new DefaultTimestampEditorFactory(DefaultTimestampEditorFactory.Mode.TIME),
            new DefaultTimestampEditorFactory(DefaultTimestampEditorFactory.Mode.DATETIME),
            new DefaultTimeEditorFactory(), new DefaultTextEditorFactory(), new DefaultBooleanEditorFactory());
    }

    public static GridCellEditorFactoryProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public @Nullable GridCellEditorFactory getEditorFactory(GridCellRequest<GridRow, GridColumn> request) {
        return GridCellEditorFactoryProvider.getEditorFactory(myDefaultFactories, factory -> factory.getSuitability(request),
            GridCellEditorFactory.class);
    }
}
