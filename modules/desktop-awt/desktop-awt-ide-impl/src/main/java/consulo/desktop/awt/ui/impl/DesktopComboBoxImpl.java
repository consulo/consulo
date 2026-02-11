/*
 * Copyright 2013-2016 consulo.io
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

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.disposer.Disposable;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.model.ListModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopComboBoxImpl<E> extends SwingComponentDelegate<DesktopComboBoxImpl.MyComboBox> implements ComboBox<E> {
    class MyComboBox<T> extends consulo.ui.ex.awt.ComboBox<T> implements FromSwingComponentWrapper {
        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopComboBoxImpl.this;
        }
    }

    private final ListModel<E> myModel;
    private TextItemRender<E> myRenderer = ListItemRenders.defaultRender();

    public DesktopComboBoxImpl(ListModel<E> model) {
        myModel = model;
    }

    @Override
    protected MyComboBox createComponent() {
        MyComboBox<E> myComponent = new MyComboBox<>();
        myComponent.setModel(new DesktopComboBoxModelWrapper<>(myModel));
        myComponent.setRenderer(new DesktopListRender<>(() -> myRenderer));
        return myComponent;
    }

    @Nonnull
    @Override
    public ListModel<E> getListModel() {
        return myModel;
    }

    @Override
    public void setRenderer(@Nonnull TextItemRender<E> renderer) {
        myRenderer = renderer;
    }

    @Override
    public void setValueByIndex(int index) {
        toAWTComponent().setSelectedIndex(index);
    }

    @RequiredUIAccess
    @Override
    public void setValue(E value, boolean fireListeners) {
        toAWTComponent().setSelectedItem(value);
    }

    @Nonnull
    @Override
    public Disposable addValueListener(@Nonnull ComponentEventListener<ValueComponent<E>, ValueComponentEvent<E>> valueListener) {
        DesktopValueListenerAsItemListenerImpl<E> listener = new DesktopValueListenerAsItemListenerImpl<>(this, valueListener, true);
        toAWTComponent().addItemListener(listener);
        return () -> toAWTComponent().removeItemListener(listener);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public E getValue() {
        return (E) toAWTComponent().getSelectedItem();
    }
}
