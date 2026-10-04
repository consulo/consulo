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
package consulo.ui.grid.color;

import consulo.ui.color.ColorValue;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridMutator.DatabaseMutator;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A color model which asks its {@link ColorLayer layers} in the order of their priority, each one given the color the layers before
 * it decided. The default layers are the backgrounds of pending changes ({@link MutationsColorLayer}); a data source adds its own,
 * such as a color per column, with {@link #addLayer(ColorLayer)}.
 */
public class GridColorModelImpl implements GridColorModel {
    private ColorLayer[] myLayers;

    private final DataGrid myGrid;

    public GridColorModelImpl(DataGrid grid, @Nullable DatabaseMutator<GridRow, GridColumn> mutator) {
        this(grid, createLayers(mutator));
    }

    public GridColorModelImpl(DataGrid grid, ColorLayer... layers) {
        myGrid = grid;
        myLayers = layers.clone();
        Arrays.sort(myLayers);
    }

    private static ColorLayer[] createLayers(@Nullable DatabaseMutator<GridRow, GridColumn> mutator) {
        return new ColorLayer[]{new MutationsColorLayer(mutator)};
    }

    /**
     * @return the first layer which is an instance of the class, or {@code null} when there is none
     */
    public @Nullable ColorLayer getLayer(Class<? extends ColorLayer> clazz) {
        return ContainerUtil.find(myLayers, clazz::isInstance);
    }

    public void removeLayer(Class<? extends ColorLayer> clazz) {
        List<ColorLayer> layers = new ArrayList<>(Arrays.asList(myLayers));
        layers.removeIf(clazz::isInstance);
        myLayers = layers.toArray(ColorLayer[]::new);
    }

    public void removeLayer(ColorLayer layer) {
        List<ColorLayer> layers = new ArrayList<>(Arrays.asList(myLayers));
        layers.remove(layer);
        myLayers = layers.toArray(ColorLayer[]::new);
    }

    public void addLayer(ColorLayer layer) {
        List<ColorLayer> layers = new ArrayList<>(Arrays.asList(myLayers));
        layers.add(layer);
        myLayers = layers.toArray(ColorLayer[]::new);
        Arrays.sort(myLayers);
    }

    @Override
    public @Nullable ColorValue getCellBackground(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        ColorValue color = null;
        for (ColorLayer layer : myLayers) {
            color = layer.getCellBackground(row, column, myGrid, color);
        }
        return color;
    }

    @Override
    public @Nullable ColorValue getCellForeground(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        ColorValue color = null;
        for (ColorLayer layer : myLayers) {
            color = layer.getCellForeground(row, column, myGrid, color);
        }
        return color;
    }

    @Override
    public @Nullable ColorValue getRowHeaderBackground(ModelIndex<GridRow> row) {
        ColorValue color = null;
        for (ColorLayer layer : myLayers) {
            color = layer.getRowHeaderBackground(row, myGrid, color);
        }
        return color;
    }
}
