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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.selection.MultiSelect;
import consulo.localize.LocalizeValue;
import consulo.ui.Length;
import consulo.web.ui.impl.internal.vaadin.WebLength;
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;
import consulo.util.collection.ContainerUtil;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebTableImpl<Item> extends VaadinComponentDelegate<WebTableImpl<Item>.Vaadin> implements Table<Item> {
    private static final String NO_HEADER_CLASS = "web-table-no-header";

    @StyleSheet("/table/webTable.css")
    public class Vaadin extends Grid<Item> implements FromVaadinComponentWrapper {
        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return WebTableImpl.this;
        }
    }

    private final FlatDataModel<Item> myModel;
    private @Nullable Function<Item, ColorValue> myRowBackgroundGetter;
    private final List<WebTableColumnImpl<Item, ?>> myColumns = new ArrayList<>();

    private @Nullable Function<Item, Length> myItemHeightGetter;
    private @Nullable Function<Item, String> mySpeedSearchConverter;
    private boolean myAllowMultipleSelect;

    public WebTableImpl(FlatDataModel<Item> model) {
        myModel = model;

        Vaadin grid = toVaadinComponent();
        grid.addThemeVariants(GridVariant.NO_ROW_BORDERS, GridVariant.COLUMN_BORDERS, GridVariant.NO_BORDER);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setItems(new ListDataProvider<>(ContainerUtil.collect(model.iterator())));

        model.addListener(this::onModelChanged);

        installSelectionListener();

        grid.addItemClickListener(event -> {
            if (myAllowMultipleSelect) {
                selectClicked(event.getItem(), event.isCtrlKey() || event.isMetaKey());
            }
        });

        grid.addItemDoubleClickListener(event ->
            getListenerDispatcher(TableDoubleClickEvent.class)
                .onEvent(new TableDoubleClickEvent(this, event.getItem(), WebInputDetails.details(event))));
    }

    private void onModelChanged(FlatDataModelEvent event) {
        Vaadin grid = toVaadinComponent();
        if (event.getType() == FlatDataModelEvent.Type.UPDATED) {
            int size = myModel.getSize();
            for (int i = Math.max(0, event.getFromIndex()); i <= event.getToIndex() && i < size; i++) {
                grid.getDataProvider().refreshItem(myModel.get(i));
            }
            return;
        }

        grid.setItems(new ListDataProvider<>(ContainerUtil.collect(myModel.iterator())));
    }

    private void selectClicked(Item item, boolean toggle) {
        MultiSelect<Grid<Item>, Item> selection = toVaadinComponent().asMultiSelect();
        Set<Item> selected = selection.getSelectedItems();
        if (toggle) {
            if (selected.contains(item)) {
                selection.deselect(item);
            }
            else {
                selection.select(item);
            }
            return;
        }

        Set<Item> removed = new HashSet<>(selected);
        removed.remove(item);
        selection.updateSelection(Set.of(item), removed);
    }

    private void installSelectionListener() {
        Vaadin grid = toVaadinComponent();
        grid.addSelectionListener(event -> {
            List<Item> selected = new ArrayList<>(event.getAllSelectedItems());

            // a ComponentRenderer is not re-run on selection change, so the rows entering and leaving
            // the selection are refreshed by hand to keep RenderItem#isSelected honest
            grid.getDataProvider().refreshAll();

            getListenerDispatcher(TableSelectEvent.class).onEvent(new TableSelectEvent(this, selected));
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    boolean isSelected(@Nullable Item item) {
        return item != null && toVaadinComponent().getSelectedItems().contains(item);
    }

    void applyItemHeight(com.vaadin.flow.component.Component component, @Nullable Item item) {
        if (myItemHeightGetter != null && item != null) {
            component.getElement().getStyle().set("height", WebLength.toCss(myItemHeightGetter.apply(item)));
        }
    }

    @Override
    public FlatDataModel<Item> getDataModel() {
        return myModel;
    }

    @Override
    public void setRowBackgroundGetter(@Nullable Function<Item, ColorValue> getter) {
        myRowBackgroundGetter = getter;
    }

    /**
     * Read by the columns while they render - the band is one rule over the row, not a copy per column.
     */
    @Nullable
    ColorValue getRowBackground(Item item) {
        Function<Item, ColorValue> getter = myRowBackgroundGetter;
        return getter == null ? null : getter.apply(item);
    }

    @Override
    public <Value> TableColumn<Item, Value> addColumn(LocalizeValue header, Function<Item, Value> valueProvider) {
        WebTableColumnImpl<Item, Value> column = new WebTableColumnImpl<>(this, valueProvider);
        myColumns.add(column);
        column.setHeader(header);
        return column;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<TableColumn<Item, ?>> getColumns() {
        return List.copyOf((List) myColumns);
    }

    @Override
    public void setAllowMultipleSelect(boolean allow) {
        if (myAllowMultipleSelect == allow) {
            return;
        }

        myAllowMultipleSelect = allow;

        Vaadin grid = toVaadinComponent();
        grid.setSelectionMode(allow ? Grid.SelectionMode.MULTI : Grid.SelectionMode.SINGLE);
        if (allow) {
            WebGridSelectionColumn.hide(grid);
        }

        installSelectionListener();
    }

    @Override
    public @Nullable Item getSelectedItem() {
        return toVaadinComponent().getSelectedItems().stream().findFirst().orElse(null);
    }

    @Override
    public List<Item> getSelectedItems() {
        return new ArrayList<>(toVaadinComponent().getSelectedItems());
    }

    @RequiredUIAccess
    @Override
    public void select(Item item) {
        toVaadinComponent().select(item);
    }

    @RequiredUIAccess
    @Override
    public void deselectAll() {
        toVaadinComponent().deselectAll();
    }

    @Override
    public void setShowHeader(boolean show) {
        // vaadin has no api for this, so a modifier class drives a rule over the header part
        if (show) {
            toVaadinComponent().removeClassName(NO_HEADER_CLASS);
        }
        else {
            toVaadinComponent().addClassName(NO_HEADER_CLASS);
        }
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Item item) {
        toVaadinComponent().scrollToItem(item);
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<Item, String> converter) {
        mySpeedSearchConverter = converter;
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<Item, Length> getter) {
        myItemHeightGetter = getter;
        toVaadinComponent().getDataProvider().refreshAll();
    }
}
