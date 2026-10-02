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

import consulo.ui.MultiSelectListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author VISTALL
 */
@SuppressWarnings("unchecked")
public class WebMultiSelectListBoxImpl<E> extends WebPooledListBoxBase<E> implements MultiSelectListBox<E> {
    private final Set<E> mySelection = new LinkedHashSet<>();
    private @Nullable E myAnchor;

    public WebMultiSelectListBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected boolean isSelected(E item) {
        return mySelection.contains(item);
    }

    @Override
    @RequiredUIAccess
    protected void onRowClicked(E item, int index, InputDetails details) {
        boolean toggle = false;
        boolean range = false;
        if (details instanceof ModifiedInputDetails modified) {
            toggle = modified.withCtrl() || modified.withMeta();
            range = modified.withShift();
        }

        FlatDataModel<E> model = getDataModel();
        int anchorIndex = range && myAnchor != null ? model.indexOf(myAnchor) : -1;

        Set<E> selection;
        if (anchorIndex >= 0 && index >= 0) {
            selection = toggle ? new LinkedHashSet<>(mySelection) : new LinkedHashSet<>();

            int from = Math.min(anchorIndex, index);
            int to = Math.min(Math.max(anchorIndex, index), model.getSize() - 1);
            for (int i = from; i <= to; i++) {
                E candidate = model.get(i);
                if (!isSeparatorItem(candidate)) {
                    selection.add(candidate);
                }
            }
        }
        else if (toggle) {
            selection = new LinkedHashSet<>(mySelection);
            if (!selection.remove(item)) {
                selection.add(item);
            }
            myAnchor = item;
        }
        else {
            selection = new LinkedHashSet<>();
            selection.add(item);
            myAnchor = item;
        }

        changeSelection(selection, true);
    }

    @Override
    @RequiredUIAccess
    protected void onRowContextPressed(E item, int index) {
        Set<E> selection = new LinkedHashSet<>();
        selection.add(item);
        myAnchor = item;

        changeSelection(selection, true);
    }

    @Override
    public List<E> getValue() {
        if (mySelection.isEmpty()) {
            return List.of();
        }

        List<E> values = new ArrayList<>(mySelection.size());
        for (E item : getDataModel()) {
            if (mySelection.contains(item) && !isSeparatorItem(item)) {
                values.add(item);
            }
        }
        return values;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable List<E> value, boolean fireListeners) {
        Set<E> selection = new LinkedHashSet<>();
        E anchor = null;
        if (value != null && !value.isEmpty()) {
            Set<E> wanted = new HashSet<>(value);
            for (E item : getDataModel()) {
                if (wanted.contains(item) && !isSeparatorItem(item)) {
                    selection.add(item);
                }
            }

            for (E item : value) {
                if (selection.contains(item)) {
                    anchor = item;
                }
            }
        }

        myAnchor = anchor;

        changeSelection(selection, fireListeners);
    }

    @RequiredUIAccess
    private void changeSelection(Set<E> selection, boolean fireListeners) {
        if (mySelection.equals(selection)) {
            return;
        }

        Set<E> previous = new HashSet<>(mySelection);

        mySelection.clear();
        mySelection.addAll(selection);

        rebindRows(item -> previous.contains(item) != mySelection.contains(item));

        if (fireListeners) {
            fireValueChanged();
        }
    }

    @RequiredUIAccess
    private void fireValueChanged() {
        getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
    }

    @Override
    @RequiredUIAccess
    public void isSeparator(Predicate<E> predicate) {
        super.isSeparator(predicate);

        if (retainPresent()) {
            fireValueChanged();
        }
    }

    @Override
    @RequiredUIAccess
    protected void onModelChanged(FlatDataModelEvent event) {
        boolean pruned = retainPresent();

        super.onModelChanged(event);

        if (pruned) {
            fireValueChanged();
        }
    }

    private boolean retainPresent() {
        if (mySelection.isEmpty()) {
            return false;
        }

        Set<E> present = new HashSet<>();
        for (E item : getDataModel()) {
            if (mySelection.contains(item) && !isSeparatorItem(item)) {
                present.add(item);
            }
        }

        if (myAnchor != null && !present.contains(myAnchor)) {
            myAnchor = null;
        }

        return mySelection.retainAll(present);
    }
}
