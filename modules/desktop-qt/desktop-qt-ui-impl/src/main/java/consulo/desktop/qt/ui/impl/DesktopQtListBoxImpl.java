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
package consulo.desktop.qt.ui.impl;

import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QListWidget;
import io.qt.widgets.QListWidgetItem;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtListBoxImpl<E> extends DesktopQtListBoxBaseImpl<E> implements ListBox<E> {
    private boolean mySelectOnHover;

    private int mySelectedIndex = -1;

    public DesktopQtListBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected void initialize(QListWidget component) {
        super.initialize(component);

        component.itemEntered.connect(item -> {
            if (mySelectOnHover) {
                component.setCurrentRow(component.row(item));
            }
        });
    }

    @Override
    protected QAbstractItemView.SelectionMode selectionMode() {
        return QAbstractItemView.SelectionMode.SingleSelection;
    }

    @Override
    protected void restoreSelection(QListWidget component) {
        if (mySelectedIndex < 0 || mySelectedIndex >= component.count()) {
            component.clearSelection();
            component.setCurrentRow(-1);
        }

        applySelectedIndex(component);
    }

    @Override
    protected void syncSelection(QListWidget component) {
        int current = component.currentRow();
        if (current >= 0) {
            mySelectedIndex = current;
        }
        else {
            applySelectedIndex(component);
        }
    }

    private void applySelectedIndex(QListWidget component) {
        int index = mySelectedIndex;

        // a list of a popup always offers a row - the awt popups open with the first one under the selection, and
        // a list which offers none answers the return key with nothing at all
        if (index < 0 || index >= component.count()) {
            index = mySelectOnHover ? firstSelectableRow() : -1;
        }

        if (index < 0) {
            return;
        }

        mySelectedIndex = index;

        component.setCurrentRow(index);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void selectionChanged(QListWidget component) {
        List<QListWidgetItem> selected = component.selectedItems();

        mySelectedIndex = selected.isEmpty() ? -1 : component.row(selected.get(0));

        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, getValue(), DesktopQtCurrentInput.current(component)));
    }

    @Override
    protected boolean isSelectedRow(int row) {
        return row == mySelectedIndex;
    }

    @Override
    protected void rowActivated(int row) {
        mySelectedIndex = row;
    }

    @Override
    @RequiredUIAccess
    public void setSelectOnHover(boolean selectOnHover) {
        mySelectOnHover = selectOnHover;
    }

    @Override
    public void setValueByIndex(int index) {
        mySelectedIndex = index;

        if (myComponent != null) {
            myComponent.setCurrentRow(index);
        }
    }

    @Override
    public @Nullable E getValue() {
        return valueAt(mySelectedIndex);
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable E value, boolean fireListeners) {
        int index = value == null ? -1 : getDataModel().indexOf(value);

        if (fireListeners) {
            setValueByIndex(index);
            return;
        }

        myRebuilding = true;
        try {
            setValueByIndex(index);
        }
        finally {
            myRebuilding = false;
        }

        layoutPool();
    }
}
