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

import consulo.ui.Length;
import consulo.ui.ComboBox;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.FlatDataModel;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtComboBoxImpl<E> extends QtComponentDelegate<QComboBox> implements ComboBox<E> {
    private TextItemRender<E> myRenderer = TextItemRender.defaultRender();

    private final FlatDataModel<E> myModel;

    private int mySelectedIndex = 0;
    private boolean myFireListeners = true;

    public DesktopQtComboBoxImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    @Override
    protected QComboBox createQt(QWidget parent) {
        return new QComboBox(parent);
    }

    @Override
    protected void initialize(QComboBox component) {
        for (int i = 0; i < myModel.getSize(); i++) {
            E element = myModel.get(i);

            DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();

            myRenderer.render(presentation, RenderItem.of(element, i == mySelectedIndex));

            component.addItem(presentation.toString());
        }

        component.setCurrentIndex(mySelectedIndex);

        component.currentIndexChanged.connect(index -> {
            mySelectedIndex = index;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, getValue(), DesktopQtCurrentInput.current(component)));
            }
        });
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myRenderer = render;
    }

    @Override
    public void setValueByIndex(int index) {
        setValueByIndex(index, true);
    }

    private void setValueByIndex(int index, boolean fireListeners) {
        mySelectedIndex = index;

        QComboBox component = myComponent;
        if (component == null) {
            return;
        }

        // the widget answers a set index with the same signal a click raises, so the guard is what keeps a
        // programmatic set - a reset writing the stored value back - from reading as a user's choice
        myFireListeners = fireListeners;
        try {
            component.setCurrentIndex(index);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public @Nullable E getValue() {
        return mySelectedIndex >= 0 && mySelectedIndex < myModel.getSize() ? myModel.get(mySelectedIndex) : null;
    }

    @Override
    @RequiredUIAccess
    public void setValue(E value, boolean fireListeners) {
        setValueByIndex(myModel.indexOf(value), fireListeners);
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<E, Length> getter) {
    }
}
