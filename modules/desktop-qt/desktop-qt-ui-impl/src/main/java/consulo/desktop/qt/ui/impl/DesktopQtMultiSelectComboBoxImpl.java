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

import consulo.localize.LocalizeValue;
import consulo.ui.MultiSelectComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtMultiSelectComboBoxImpl<E> extends DesktopQtComboBoxBaseImpl<E, DesktopQtMultiSelectComboBox>
    implements MultiSelectComboBox<E> {
    private static final String SEPARATOR = ", ";

    private final Set<E> mySelection = new HashSet<>();
    private List<E> myLastValue = List.of();
    private LocalizeValue myPlaceholder = LocalizeValue.empty();

    public DesktopQtMultiSelectComboBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected DesktopQtMultiSelectComboBox createQt(QWidget parent) {
        return new DesktopQtMultiSelectComboBox(parent);
    }

    @Override
    protected void initialize(DesktopQtMultiSelectComboBox component) {
        component.setToggleHandler(row -> toggle(component, row));
        component.setPlaceholder(myPlaceholder.get());

        rebuild(component);
    }

    @Override
    protected boolean isSelectedItem(int index, E element) {
        return mySelection.contains(element);
    }

    @Override
    protected void afterRebuild(DesktopQtMultiSelectComboBox component) {
        component.setCurrentIndex(-1);

        applySelection(component);
    }

    @Override
    @RequiredUIAccess
    protected void onModelChanged() {
        retainPresent();

        super.onModelChanged();

        fireIfChanged(myComponent);
    }

    @RequiredUIAccess
    private void toggle(DesktopQtMultiSelectComboBox component, int row) {
        if (row < 0 || row >= myModel.getSize()) {
            return;
        }

        E element = myModel.get(row);
        boolean checked = !mySelection.remove(element);
        if (checked) {
            mySelection.add(element);
        }

        component.setItemChecked(row, checked);
        component.setSelectionText(selectionText(component));

        fireIfChanged(component);
    }

    private void applySelection(DesktopQtMultiSelectComboBox component) {
        int count = Math.min(component.count(), myModel.getSize());
        for (int row = 0; row < count; row++) {
            component.setItemChecked(row, mySelection.contains(myModel.get(row)));
        }

        component.setSelectionText(selectionText(component));
    }

    private String selectionText(DesktopQtMultiSelectComboBox component) {
        StringBuilder builder = new StringBuilder();

        int count = Math.min(component.count(), myModel.getSize());
        for (int row = 0; row < count; row++) {
            if (!mySelection.contains(myModel.get(row))) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(SEPARATOR);
            }
            builder.append(component.itemText(row));
        }
        return builder.toString();
    }

    private void retainPresent() {
        if (mySelection.isEmpty()) {
            return;
        }

        Set<E> present = new HashSet<>();
        for (E item : myModel) {
            if (mySelection.contains(item)) {
                present.add(item);
            }
        }
        mySelection.retainAll(present);
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text;

        DesktopQtMultiSelectComboBox component = myComponent;
        if (component != null && !component.isDisposed()) {
            component.setPlaceholder(text.get());
        }
    }

    @Override
    public List<E> getValue() {
        if (mySelection.isEmpty()) {
            return List.of();
        }

        List<E> values = new ArrayList<>(mySelection.size());
        for (E item : myModel) {
            if (mySelection.contains(item)) {
                values.add(item);
            }
        }
        return values;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable List<E> value, boolean fireListeners) {
        mySelection.clear();
        if (value != null) {
            for (E item : value) {
                if (myModel.indexOf(item) >= 0) {
                    mySelection.add(item);
                }
            }
        }

        DesktopQtMultiSelectComboBox component = myComponent;
        if (component != null && !component.isDisposed()) {
            applySelection(component);
        }

        if (fireListeners) {
            fireIfChanged(component);
        }
        else {
            myLastValue = getValue();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireIfChanged(@Nullable QWidget component) {
        List<E> value = getValue();
        if (value.equals(myLastValue)) {
            return;
        }

        myLastValue = value;

        QWidget widget = component != null && !component.isDisposed() ? component : null;
        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, value, DesktopQtCurrentInput.current(widget)));
    }
}
