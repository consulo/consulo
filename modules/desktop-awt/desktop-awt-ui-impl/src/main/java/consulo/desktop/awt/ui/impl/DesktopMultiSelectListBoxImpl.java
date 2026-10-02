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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.disposer.Disposable;
import consulo.ui.MultiSelectListBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.awt.JBList;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;
import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
class DesktopMultiSelectListBoxImpl<E> extends DesktopListBoxBaseImpl<E, List<E>> implements MultiSelectListBox<E> {
    private class MySelectionModel extends DefaultListSelectionModel {
        private final JBList<E> myList;

        MySelectionModel(JBList<E> list) {
            myList = list;
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (index0 == index1 && index0 >= 0) {
                int index = findSelectableIndex(myList.getModel(), index0, getLeadSelectionIndex());
                if (index >= 0) {
                    super.setSelectionInterval(index, index);
                }
                return;
            }

            selectSkippingSeparators(index0, index1, () -> super.setSelectionInterval(index0, index1));
        }

        @Override
        public void addSelectionInterval(int index0, int index1) {
            if (index0 == index1 && isSeparatorIndex(index0)) {
                return;
            }

            selectSkippingSeparators(index0, index1, () -> super.addSelectionInterval(index0, index1));
        }

        private void selectSkippingSeparators(int index0, int index1, Runnable select) {
            boolean adjusting = getValueIsAdjusting();
            setValueIsAdjusting(true);
            try {
                select.run();

                boolean removed = false;
                for (int i = Math.min(index0, index1); i <= Math.max(index0, index1); i++) {
                    if (isSeparatorIndex(i) && isSelectedIndex(i)) {
                        removeSelectionInterval(i, i);
                        removed = true;
                    }
                }

                if (removed) {
                    setAnchorSelectionIndex(index0);
                    moveLeadSelectionIndex(index1);
                }
            }
            finally {
                setValueIsAdjusting(adjusting);
            }
        }

        private boolean isSeparatorIndex(int index) {
            javax.swing.ListModel<E> model = myList.getModel();
            return index >= 0 && index < model.getSize() && isSeparatorItem(model.getElementAt(index));
        }
    }

    private boolean mySuppressValueEvents;
    private List<E> myLastValue = List.of();

    public DesktopMultiSelectListBoxImpl(FlatDataModel<E> model) {
        super(model);

        model.addListener(this::onModelChanged);
    }

    @Override
    protected JBList<E> createComponent() {
        JBList<E> component = super.createComponent();
        component.addListSelectionListener(this::onSelectionChanged);
        return component;
    }

    @Override
    protected ListSelectionModel createSelectionModel(JBList<E> component) {
        return new MySelectionModel(component);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Disposable addValueListener(ComponentEventListener<ValueComponent<List<E>>, ValueComponentEvent<List<E>>> valueListener) {
        return addListener((Class) ValueComponentEvent.class, valueListener);
    }

    @RequiredUIAccess
    private void onSelectionChanged(ListSelectionEvent event) {
        if (mySuppressValueEvents || event.getValueIsAdjusting()) {
            return;
        }

        fireIfChanged(DesktopAWTInputDetails.currentEvent(toAWTComponent()));
    }

    @RequiredUIAccess
    private void onModelChanged(FlatDataModelEvent event) {
        if (!isInitialized()) {
            return;
        }

        FlatDataModelEvent.Type type = event.getType();
        if (type == FlatDataModelEvent.Type.RESET || type == FlatDataModelEvent.Type.UPDATED) {
            applyValue(myLastValue, true, false);
        }
    }

    @Override
    @RequiredUIAccess
    public void isSeparator(Predicate<E> predicate) {
        super.isSeparator(predicate);

        if (isInitialized()) {
            applyValue(myLastValue, true, false);
        }
    }

    @RequiredUIAccess
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireIfChanged(@Nullable InputDetails inputDetails) {
        List<E> value = getValue();
        if (value.equals(myLastValue)) {
            return;
        }

        myLastValue = value;

        ValueComponentEvent<List<E>> event = inputDetails == null
            ? new ValueComponentEvent<>(this, value)
            : new ValueComponentEvent<>(this, value, inputDetails);
        getListenerDispatcher(ValueComponentEvent.class).onEvent(event);
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable List<E> value, boolean fireListeners) {
        applyValue(value, fireListeners, true);
    }

    @RequiredUIAccess
    private void applyValue(@Nullable List<E> value, boolean fireListeners, boolean scrollToFirst) {
        JBList<E> list = toAWTComponent();
        ListSelectionModel selectionModel = list.getSelectionModel();

        boolean suppress = mySuppressValueEvents;
        boolean adjusting = selectionModel.getValueIsAdjusting();
        mySuppressValueEvents = true;
        selectionModel.setValueIsAdjusting(true);
        try {
            selectionModel.clearSelection();

            int firstIndex = -1;
            if (value != null) {
                for (E item : value) {
                    int index = myModel.indexOf(item);
                    if (index >= 0 && !isSeparatorItem(item)) {
                        selectionModel.addSelectionInterval(index, index);
                        firstIndex = firstIndex < 0 ? index : Math.min(firstIndex, index);
                    }
                }
            }

            if (scrollToFirst && firstIndex >= 0) {
                list.ensureIndexIsVisible(firstIndex);
            }
        }
        finally {
            selectionModel.setValueIsAdjusting(adjusting);
            mySuppressValueEvents = suppress;
        }

        if (fireListeners && !suppress) {
            fireIfChanged(null);
        }
        else {
            myLastValue = getValue();
        }
    }

    @Override
    public List<E> getValue() {
        List<E> values = new ArrayList<>();
        for (E value : toAWTComponent().getSelectedValuesList()) {
            if (value != null && !isSeparatorItem(value)) {
                values.add(value);
            }
        }
        return values;
    }
}
