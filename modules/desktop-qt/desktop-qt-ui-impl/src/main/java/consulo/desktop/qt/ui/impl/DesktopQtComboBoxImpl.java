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

import consulo.ui.ComboBox;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.FlatDataModel;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtComboBoxImpl<E> extends QtComponentDelegate<QComboBox> implements ComboBox<E> {
    private TextItemRender<E> myRenderer = TextItemRender.defaultRender();

    private final FlatDataModel<E> myModel;

    private int mySelectedIndex = 0;

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

        component.currentIndexChanged.connect(index -> mySelectedIndex = index);
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
        mySelectedIndex = index;

        if (myComponent != null) {
            myComponent.setCurrentIndex(index);
        }
    }

    @Override
    public @Nullable E getValue() {
        return myModel.get(mySelectedIndex);
    }

    @Override
    @RequiredUIAccess
    public void setValue(E value, boolean fireListeners) {
        setValueByIndex(myModel.indexOf(value));
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
    public void setItemHeightGetter(@Nullable ToIntFunction<E> getter) {
    }
}
