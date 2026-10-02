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

import consulo.ui.MultiSelectListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import io.qt.core.QItemSelectionModel;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QListWidget;
import io.qt.widgets.QListWidgetItem;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public class DesktopQtMultiSelectListBoxImpl<E> extends DesktopQtListBoxBaseImpl<E> implements MultiSelectListBox<E> {
    private final Set<E> mySelected = new HashSet<>();
    private List<E> myLastValue = List.of();
    private boolean myApplyingValue;

    public DesktopQtMultiSelectListBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected QAbstractItemView.SelectionMode selectionMode() {
        return QAbstractItemView.SelectionMode.ExtendedSelection;
    }

    @Override
    protected void restoreSelection(QListWidget component) {
        component.clearSelection();

        int first = -1;
        for (int row = 0; row < component.count(); row++) {
            E value = valueAt(row);
            if (value == null || !mySelected.contains(value)) {
                continue;
            }

            QListWidgetItem item = component.item(row);
            if (item == null) {
                continue;
            }

            item.setSelected(true);

            if (first < 0) {
                first = row;
            }
        }

        QListWidgetItem current = component.currentItem();
        if (first >= 0 && (current == null || !current.isSelected())) {
            component.setCurrentRow(first, QItemSelectionModel.SelectionFlag.NoUpdate);
        }

        readSelection(component);

        fireIfChanged(component);
    }

    @Override
    protected void syncSelection(QListWidget component) {
        readSelection(component);

        fireIfChanged(component);
    }

    @Override
    protected void selectionChanged(QListWidget component) {
        readSelection(component);

        layoutPool();

        fireIfChanged(component);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireIfChanged(@Nullable QListWidget component) {
        if (myApplyingValue) {
            return;
        }

        List<E> value = getValue();
        if (value.equals(myLastValue)) {
            return;
        }

        myLastValue = value;

        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, value, DesktopQtCurrentInput.current(component)));
    }

    private void readSelection(QListWidget component) {
        mySelected.clear();

        for (QListWidgetItem item : component.selectedItems()) {
            E value = valueAt(component.row(item));
            if (value != null) {
                mySelected.add(value);
            }
        }
    }

    @Override
    protected boolean isSelectedRow(int row) {
        E value = valueAt(row);
        return value != null && mySelected.contains(value);
    }

    @Override
    public List<E> getValue() {
        if (mySelected.isEmpty()) {
            return List.of();
        }

        List<E> values = new ArrayList<>();
        for (int row = 0; row < getDataModel().getSize(); row++) {
            E value = valueAt(row);
            if (value != null && mySelected.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setValue(@Nullable List<E> value, boolean fireListeners) {
        mySelected.clear();
        if (value != null) {
            FlatDataModel<E> model = getDataModel();
            for (E item : value) {
                if (valueAt(model.indexOf(item)) != null) {
                    mySelected.add(item);
                }
            }
        }

        QListWidget component = myComponent;
        if (component != null && !component.isDisposed()) {
            myRebuilding = true;
            myApplyingValue = true;
            try {
                restoreSelection(component);
            }
            finally {
                myApplyingValue = false;
                myRebuilding = false;
            }

            layoutPool();
        }

        if (fireListeners) {
            fireIfChanged(component);
        }
        else {
            myLastValue = getValue();
        }
    }
}
