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
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.disposer.Disposable;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ListDoubleClickEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.event.DoubleClickListener;

import javax.swing.DefaultListSelectionModel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awt.speedSearch.ListSpeedSearch;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.function.Function;
import java.util.function.Predicate;
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
    private Predicate<E> mySeparatorPredicate = item -> false;

    public DesktopListBoxImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    @Override
    protected JBList<E> createComponent() {
        MyJBList<E> component = new MyJBList<>(new DesktopFlatDataModelWrapper<>(myModel));
        applyRender(component);
        applySpeedSearch(component);
        applySeparatorSelection(component);
        applyDoubleClick(component);
        return component;
    }

    private void applyDoubleClick(JBList<E> component) {
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                int index = component.locationToIndex(event.getPoint());
                if (index < 0) {
                    return false;
                }

                Rectangle bounds = component.getCellBounds(index, index);
                if (bounds == null || !bounds.contains(event.getPoint())) {
                    return false;
                }

                E value = component.getModel().getElementAt(index);
                if (mySeparatorPredicate.test(value)) {
                    return false;
                }

                getListenerDispatcher(ListDoubleClickEvent.class)
                    .onEvent(new ListDoubleClickEvent(DesktopListBoxImpl.this, value));
                return true;
            }
        }.installOn(component);
    }

    private void applyRender(JBList<E> component) {
        ListCellRenderer<E> render = myComponentRender != null
            ? new DesktopComponentItemRenderAdapter<>(myComponentRender)
            : new DesktopListRender<>(() -> myTextRender);

        ListCellRenderer<E> withHeight = DesktopItemHeightRender.wrap(render, () -> myItemHeightGetter);

        // the swing popups do the same - the renderer answers a separator with a component of its own rather than
        // with a row, see GroupedItemsListRenderer
        component.setCellRenderer((list, value, index, selected, focused) -> {
            if (value != null && mySeparatorPredicate.test(value)) {
                TitledSeparator separator = new TitledSeparator();
                separator.setBorder(JBUI.Borders.empty());
                separator.setOpaque(false);
                return separator;
            }
            return withHeight.getListCellRendererComponent(list, value, index, selected, focused);
        });
    }

    @Override
    public void isSeparator(Predicate<E> predicate) {
        mySeparatorPredicate = predicate;
        if (isRealized()) {
            applyRender(toAWTComponent());
            applySeparatorSelection(toAWTComponent());
        }
    }

    /**
     * The swing popups move over a separator rather than onto it - see {@code ListPopupImpl.MyListSelectionModel}.
     */
    private void applySeparatorSelection(JBList<E> component) {
        component.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                javax.swing.ListModel<E> model = component.getModel();

                if (index0 > getLeadSelectionIndex()) {
                    for (int i = index0; i < model.getSize(); i++) {
                        if (!mySeparatorPredicate.test(model.getElementAt(i))) {
                            super.setSelectionInterval(i, i);
                            return;
                        }
                    }
                }
                else {
                    for (int i = index0; i >= 0; i--) {
                        if (!mySeparatorPredicate.test(model.getElementAt(i))) {
                            super.setSelectionInterval(i, i);
                            return;
                        }
                    }
                }
            }
        });
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
