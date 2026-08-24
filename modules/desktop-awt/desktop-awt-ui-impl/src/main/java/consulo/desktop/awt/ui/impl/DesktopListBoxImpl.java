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

import consulo.ui.Length;
import consulo.ui.TransferHandler;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.DesktopListRenderInteraction.ClickTarget;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ContextMenuEvent;
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
import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
class DesktopListBoxImpl<E> extends SwingComponentDelegate<JBList<E>> implements ListBox<E> {
    private @Nullable TransferHandler<E> myTransferHandler;
    private boolean mySelectOnHover;
    private int myHoverIndex = -1;
    private int myCursorIndex = -1;

    class MyJBList extends JBList<E> implements FromSwingComponentWrapper {
        MyJBList(javax.swing.ListModel<E> dataModel) {
            super(dataModel);
            enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_EXITED) {
                setHoverIndex(-1);
            }

            if (e.isPopupTrigger()) {
                ClickTarget menuTarget = findTarget(e.getPoint(), ContextMenuEvent.class);
                if (menuTarget != null) {
                    DesktopListRenderInteraction.contextMenu(menuTarget, this, e);
                    e.consume();
                    return;
                }
            }

            ClickTarget target = SwingUtilities.isLeftMouseButton(e) ? findTarget(e.getPoint(), ClickEvent.class) : null;
            if (target != null) {
                if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                    DesktopListRenderInteraction.click(target, this, e);
                }

                if (!target.rowRoot()) {
                    e.consume();
                    return;
                }
            }

            super.processMouseEvent(e);
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_MOVED) {
                onMouseMoved(e);
            }

            super.processMouseMotionEvent(e);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return myRenderAdapter != null || super.getScrollableTracksViewportWidth();
        }

        @Override
        public Component toUIComponent() {
            return DesktopListBoxImpl.this;
        }

        private void onMouseMoved(MouseEvent e) {
            int index = rowAt(e.getPoint());

            if (mySelectOnHover) {
                if (index >= 0) {
                    setSelectedIndex(index);
                }
            }
            else {
                setHoverIndex(index);
            }

            if (index != myCursorIndex) {
                myCursorIndex = index;
                updateCursor(e);
            }
        }

        private void updateCursor(MouseEvent e) {
            boolean clickable = findTarget(e.getPoint(), ClickEvent.class) != null;
            setCursor(clickable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
        }

        private @Nullable ClickTarget findTarget(Point point, Class<? extends ComponentEvent<?>> eventClass) {
            if (myHitTestRenderer == null || myRenderAdapter == null || !myRenderAdapter.isMouseEventsAllowed()) {
                return null;
            }

            return DesktopListRenderInteraction.findTarget(this, myHitTestRenderer, point, eventClass);
        }

        private void setHoverIndex(int index) {
            if (myHoverIndex == index) {
                return;
            }

            int previous = myHoverIndex;
            myHoverIndex = index;

            repaintRow(previous);
            repaintRow(index);
        }

        private void repaintRow(int index) {
            if (index < 0) {
                return;
            }

            Rectangle bounds = getCellBounds(index, index);
            if (bounds != null) {
                repaint(bounds);
            }
        }

        private int rowAt(Point point) {
            int index = locationToIndex(point);
            if (index < 0) {
                return -1;
            }

            Rectangle bounds = getCellBounds(index, index);
            return bounds != null && bounds.contains(point) ? index : -1;
        }
    }

    private final FlatDataModel<E> myModel;

    private TextItemRender<E> myTextRender = TextItemRender.defaultRender();
    private @Nullable ComponentItemRender<E> myComponentRender;
    private @Nullable DesktopComponentItemRenderAdapter<E> myRenderAdapter;
    private @Nullable ListCellRenderer<E> myHitTestRenderer;
    private @Nullable Function<E, String> mySpeedSearchConverter;
    private @Nullable Function<E, Length> myItemHeightGetter;
    private Predicate<E> mySeparatorPredicate = item -> false;

    public DesktopListBoxImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    @Override
    protected JBList<E> createComponent() {
        MyJBList component = new MyJBList(new DesktopFlatDataModelWrapper<>(myModel));
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
                    .onEvent(new ListDoubleClickEvent(DesktopListBoxImpl.this, value, DesktopAWTInputDetails.convert(component, event)));
                return true;
            }
        }.installOn(component);
    }

    private void applyRender(JBList<E> component) {
        myRenderAdapter = myComponentRender == null ? null : new DesktopComponentItemRenderAdapter<>(myComponentRender, () -> myHoverIndex);

        ListCellRenderer<E> render = myRenderAdapter != null ? myRenderAdapter : new DesktopListRender<>(() -> myTextRender);

        ListCellRenderer<E> withHeight = DesktopLengthRender.wrap(render, () -> myItemHeightGetter);

        // the swing popups do the same - the renderer answers a separator with a component of its own rather than
        // with a row, see GroupedItemsListRenderer
        myHitTestRenderer = (list, value, index, selected, focused) -> {
            if (value != null && mySeparatorPredicate.test(value)) {
                TitledSeparator separator = new TitledSeparator();
                separator.setBorder(JBUI.Borders.empty());
                separator.setOpaque(false);
                return separator;
            }
            return withHeight.getListCellRendererComponent(list, value, index, selected, focused);
        };

        component.setCellRenderer(myHitTestRenderer);
    }

    @Override
    public void isSeparator(Predicate<E> predicate) {
        mySeparatorPredicate = predicate;
        if (isInitialized()) {
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
        if (isInitialized()) {
            applyRender(toAWTComponent());
        }
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        myComponentRender = render;
        if (isInitialized()) {
            applyRender(toAWTComponent());
        }
    }

    @Override
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
    public void setItemHeightGetter(@Nullable Function<E, Length> getter) {
        myItemHeightGetter = getter;
        if (isInitialized()) {
            toAWTComponent().setFixedCellHeight(-1);
        }
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        toAWTComponent().getEmptyText().setText(text.get());
    }

    @Override
    public void setSelectOnHover(boolean selectOnHover) {
        mySelectOnHover = selectOnHover;
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
