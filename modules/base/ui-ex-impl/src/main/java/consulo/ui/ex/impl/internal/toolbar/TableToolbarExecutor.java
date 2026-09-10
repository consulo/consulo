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
package consulo.ui.ex.impl.internal.toolbar;

import consulo.ui.Table;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.ToolbarExecutor;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Add and edit stay closed here, the same way they are for a list - a row of a table is made of as many
 * values as it has columns, so what a new one should hold is the caller's to say through its own action.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public class TableToolbarExecutor<E> implements ToolbarExecutor<E> {
    private final Table<E> myTable;

    public TableToolbarExecutor(Table<E> table) {
        myTable = table;
    }

    @Override
    @RequiredUIAccess
    public @Nullable E getSelectedValue() {
        return myTable.getSelectedItem();
    }

    @Override
    @RequiredUIAccess
    public List<E> getSelectedValues() {
        return myTable.getSelectedItems();
    }

    @Override
    @RequiredUIAccess
    public boolean canAdd() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void add() {
    }

    @Override
    @RequiredUIAccess
    public boolean canEdit() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void edit() {
    }

    @Override
    @RequiredUIAccess
    public boolean canRemove() {
        return model() != null && !myTable.getSelectedItems().isEmpty();
    }

    @Override
    @RequiredUIAccess
    public void remove() {
        MutableFlatDataModel<E> model = model();
        if (model == null) {
            return;
        }

        // the selection is read once - removing a row moves the ones after it, so asking the table again
        // between removals would walk off the rows still to go
        for (E item : List.copyOf(myTable.getSelectedItems())) {
            model.remove(item);
        }
    }

    @Override
    @RequiredUIAccess
    public boolean canMoveUp() {
        return selectedIndex() > 0;
    }

    @Override
    @RequiredUIAccess
    public void moveUp() {
        move(-1);
    }

    @Override
    @RequiredUIAccess
    public boolean canMoveDown() {
        MutableFlatDataModel<E> model = model();
        int index = selectedIndex();
        return model != null && index >= 0 && index < model.getSize() - 1;
    }

    @Override
    @RequiredUIAccess
    public void moveDown() {
        move(1);
    }

    @RequiredUIAccess
    private void move(int delta) {
        MutableFlatDataModel<E> model = model();
        E value = myTable.getSelectedItem();
        if (model == null || value == null) {
            return;
        }

        int index = model.indexOf(value);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= model.getSize()) {
            return;
        }

        model.remove(value);
        model.add(value, target);
        myTable.select(value);
    }

    @RequiredUIAccess
    private int selectedIndex() {
        MutableFlatDataModel<E> model = model();
        E value = myTable.getSelectedItem();
        return model == null || value == null ? -1 : model.indexOf(value);
    }

    private @Nullable MutableFlatDataModel<E> model() {
        FlatDataModel<E> model = myTable.getDataModel();
        return model instanceof MutableFlatDataModel<E> mutable ? mutable : null;
    }
}
