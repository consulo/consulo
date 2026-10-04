// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid.editor;

import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public interface GridCellEditorFactoryProvider {
    Key<GridCellEditorFactoryProvider> FACTORY_PROVIDER_KEY = Key.create("FACTORY_PROVIDER_KEY");

    @Nullable
    GridCellEditorFactory getEditorFactory(GridCellRequest<GridRow, GridColumn> request);

    static void set(DataGrid grid, @Nullable GridCellEditorFactoryProvider provider) {
        set((CoreGrid<GridRow, GridColumn>) grid, provider);
    }

    static void set(CoreGrid<GridRow, GridColumn> grid, @Nullable GridCellEditorFactoryProvider provider) {
        grid.putUserData(FACTORY_PROVIDER_KEY, provider);
    }

    static @Nullable GridCellEditorFactoryProvider get(CoreGrid<GridRow, GridColumn> grid) {
        return grid.getUserData(FACTORY_PROVIDER_KEY);
    }

    static @Nullable GridCellEditorFactory provideEditorFactory(GridCellRequest<GridRow, GridColumn> request) {
        GridCellEditorFactoryProvider provider = get(request.getGrid());
        return provider == null ? null : provider.getEditorFactory(request);
    }

    static <T> @Nullable T getEditorFactory(List<? extends GridCellEditorFactory> factories,
                                            Function<T, Integer> suitabilityCheck,
                                            Class<T> clazz) {
        int maxSuitability = GridCellEditorFactory.SUITABILITY_UNSUITABLE;
        T bestMatchingFactory = null;

        for (GridCellEditorFactory factory : factories) {
            if (!clazz.isAssignableFrom(factory.getClass())) {
                continue;
            }
            //noinspection unchecked
            T f = (T) factory;
            int suitability = suitabilityCheck.apply(f);
            if (suitability > maxSuitability) {
                maxSuitability = suitability;
                bestMatchingFactory = f;
            }
        }

        return bestMatchingFactory;
    }
}
