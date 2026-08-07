/*
 * Copyright 2013-2017 consulo.io
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

import consulo.ui.TransferHandler;
import consulo.desktop.awt.internal.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.disposer.Disposable;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.speedSearch.ListSpeedSearch;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
class DesktopListBoxImpl<E> extends SwingComponentDelegate<JBList<E>> implements ListBox<E> {
    private @Nullable TransferHandler<E> myTransferHandler;
    class MyJBList<T> extends JBList<T> implements FromSwingComponentWrapper {
        MyJBList(javax.swing.ListModel<T> dataModel) {
            super(dataModel);
        }

        @Override
        public Component toUIComponent() {
            return DesktopListBoxImpl.this;
        }
    }

    private final FlatDataModel<E> myModel;

    private TextItemRender<E> myTextRender = TextItemRender.defaultRender();
    private @Nullable ComponentItemRender<E> myComponentRender;
    private @Nullable Function<E, String> mySpeedSearchConverter;
    private @Nullable ToIntFunction<E> myItemHeightGetter;

    public DesktopListBoxImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    @Override
    protected JBList<E> createComponent() {
        MyJBList<E> component = new MyJBList<>(new DesktopFlatDataModelWrapper<>(myModel));
        applyRender(component);
        applySpeedSearch(component);
        return component;
    }

    private void applyRender(JBList<E> component) {
        ListCellRenderer<E> render = myComponentRender != null
            ? new DesktopComponentItemRenderAdapter<>(myComponentRender)
            : new DesktopListRender<>(() -> myTextRender);

        component.setCellRenderer(DesktopItemHeightRender.wrap(render, () -> myItemHeightGetter));
    }

    private void applySpeedSearch(JBList<E> component) {
        if (mySpeedSearchConverter != null) {
            ListSpeedSearch.installOn(component, mySpeedSearchConverter::apply);
        }
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myTextRender = render;
        myComponentRender = null;
        if (isRealized()) {
            applyRender(toAWTComponent());
        }
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        myComponentRender = render;
        if (isRealized()) {
            applyRender(toAWTComponent());
        }
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
        mySpeedSearchConverter = converter;
        if (isRealized()) {
            applySpeedSearch(toAWTComponent());
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        if (!isRealized()) {
            return null;
        }
        SpeedSearchSupply supply = SpeedSearchSupply.getSupply(toAWTComponent());
        return supply == null ? null : supply.getEnteredPrefix();
    }

    @Override
    public void setItemHeightGetter(@Nullable ToIntFunction<E> getter) {
        myItemHeightGetter = getter;
        if (isRealized()) {
            toAWTComponent().setFixedCellHeight(-1);
        }
    }

    @Override
    public void setVisibleRowCount(int count) {
        if (count > 0) {
            toAWTComponent().setVisibleRowCount(count);
        }
    }

    @Override
    public void setValueByIndex(int index) {
        toAWTComponent().setSelectedIndex(index);
    }

    @RequiredUIAccess
    @Override
    public void setValue(E value, boolean fireListeners) {
        toAWTComponent().setSelectedValue(value, true);
    }

    @Override
    public Disposable addValueListener(ComponentEventListener<ValueComponent<E>, ValueComponentEvent<E>> valueListener) {
        DesktopValueListenerAsListSelectionListener<E> listener =
            new DesktopValueListenerAsListSelectionListener<>(this, toAWTComponent(), valueListener);
        toAWTComponent().addListSelectionListener(listener);
        return () -> toAWTComponent().removeListSelectionListener(listener);
    }

    @Override
    public E getValue() {
        return toAWTComponent().getSelectedValue();
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<E> handler) {
        myTransferHandler = handler;
        toAWTComponent().setTransferHandler(handler == null ? null : new DesktopAWTTransferHandlerAdapter<>(this, handler));
    }

    @Override
    public @Nullable TransferHandler<E> getTransferHandler() {
        return myTransferHandler;
    }
}
