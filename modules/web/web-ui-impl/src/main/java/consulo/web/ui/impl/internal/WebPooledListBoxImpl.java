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

import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
@SuppressWarnings("unchecked")
public class WebPooledListBoxImpl<E> extends WebPooledListBoxBase<E> implements ListBox<E> {
    private @Nullable E myValue;

    public WebPooledListBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected boolean isSelected(E item) {
        return item.equals(myValue);
    }

    @Override
    @RequiredUIAccess
    protected void onRowClicked(E item, int index, InputDetails details) {
        setValue(item);
    }

    @Override
    @RequiredUIAccess
    protected void onRowContextPressed(E item, int index) {
        setValue(item);
    }

    @Override
    public @Nullable E getValue() {
        return myValue;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable E value, boolean fireListeners) {
        if (myValue == value) {
            return;
        }

        E previous = myValue;
        myValue = value;

        // exactly two rows change - the one losing the mark and the one taking it
        rebindRows(item -> item.equals(previous) || item.equals(value));

        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
        }
    }

    @Override
    @RequiredUIAccess
    public void setValueByIndex(int index) {
        FlatDataModel<E> model = getDataModel();
        if (index >= 0 && index < model.getSize()) {
            setValue(model.get(index));
        }
    }
}
