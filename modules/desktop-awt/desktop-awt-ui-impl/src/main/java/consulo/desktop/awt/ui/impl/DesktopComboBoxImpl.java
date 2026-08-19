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

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.disposer.Disposable;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.ComboboxSpeedSearch;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopComboBoxImpl<E> extends SwingComponentDelegate<DesktopComboBoxImpl.MyComboBox> implements ComboBox<E> {
    class MyComboBox<T> extends consulo.ui.ex.awt.ComboBox<T> implements FromSwingComponentWrapper {
        
        @Override
        public Component toUIComponent() {
            return DesktopComboBoxImpl.this;
        }
    }

    private final FlatDataModel<E> myModel;

    private TextItemRender<E> myTextRender = TextItemRender.defaultRender();
    private @Nullable ComponentItemRender<E> myComponentRender;
    private @Nullable Function<E, String> mySpeedSearchConverter;
    private @Nullable ToIntFunction<E> myItemHeightGetter;

    public DesktopComboBoxImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    @Override
    protected MyComboBox createComponent() {
        MyComboBox<E> myComponent = new MyComboBox<>();
        myComponent.setModel(new DesktopFlatDataModelWrapper<>(myModel));
        applyRender(myComponent);
        applySpeedSearch(myComponent);
        return myComponent;
    }

    @SuppressWarnings("unchecked")
    private void applyRender(MyComboBox<E> component) {
        ListCellRenderer<E> render = myComponentRender != null
            ? new DesktopComponentItemRenderAdapter<>(myComponentRender)
            : new DesktopListRender<>(() -> myTextRender);

        component.setRenderer(DesktopItemHeightRender.wrap(render, () -> myItemHeightGetter));
    }

    private void applySpeedSearch(MyComboBox<E> component) {
        if (mySpeedSearchConverter != null) {
            ComboboxSpeedSearch.installSpeedSearch(component, mySpeedSearchConverter::apply);
        }
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setRender(TextItemRender<E> render) {
        myTextRender = render;
        myComponentRender = null;
        if (isInitialized()) {
            applyRender(toAWTComponent());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setRender(ComponentItemRender<E> render) {
        myComponentRender = render;
        if (isInitialized()) {
            applyRender(toAWTComponent());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
        mySpeedSearchConverter = converter;
        if (isInitialized()) {
            applySpeedSearch(toAWTComponent());
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        if (!isInitialized()) {
            return null;
        }
        SpeedSearchSupply supply = SpeedSearchSupply.getSupply(toAWTComponent());
        return supply == null ? null : supply.getEnteredPrefix();
    }

    @Override
    public void setItemHeightGetter(@Nullable ToIntFunction<E> getter) {
        myItemHeightGetter = getter;
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

    
    @Override
    public Disposable addValueListener(ComponentEventListener<ValueComponent<E>, ValueComponentEvent<E>> valueListener) {
        DesktopValueListenerAsItemListenerImpl<E> listener = new DesktopValueListenerAsItemListenerImpl<>(this, valueListener, true);
        toAWTComponent().addItemListener(listener);
        return () -> toAWTComponent().removeItemListener(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable E getValue() {
        return (E) toAWTComponent().getSelectedItem();
    }
}
