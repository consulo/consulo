// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.util;

import com.intellij.ui.ExpandedItemListCellRendererWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;

/**
 * Materializes a concrete component in a hovered list cell that matches the ones painted by the renderer
 */
public final class JListHoveredRowMaterialiser<T> {
    private final JList<T> myList;
    private final ListCellRenderer<T> myCellRenderer;
    private int myHoveredIndex = -1;
    private @Nullable Component myRendererComponent;

    @ApiStatus.Internal
    public boolean resetCellBoundsOnHover = true;

    private JListHoveredRowMaterialiser(@Nonnull JList<T> list, @Nonnull ListCellRenderer<T> cellRenderer) {
        myList = list;
        myCellRenderer = cellRenderer;
    }

    private void setHoveredIndex(int index) {
        int oldValue = myHoveredIndex;
        myHoveredIndex = index;

        if (oldValue > 0 && oldValue < myList.getModel().getSize()) {
            Rectangle bounds = myList.getCellBounds(oldValue, oldValue);
            myList.repaint(bounds);
        }

        if (index != oldValue) {
            materialiseRendererAt(index);
        }
    }

    private void setRendererComponent(@Nullable Component component) {
        Component oldValue = myRendererComponent;
        myRendererComponent = component;

        if (oldValue != null) {
            myList.remove(oldValue);
        }

        if (component != null) {
            myList.add(component);
            component.validate();
            component.repaint();
        }
    }

    private final MouseMotionAdapter listRowHoverListener = new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            Point point = e.getPoint();
            int idx = myList.locationToIndex(point);

            if (idx >= 0 && myList.getCellBounds(idx, idx).contains(point)) {
                setHoveredIndex(idx);
            }
            else {
                setHoveredIndex(-1);
            }
        }
    };

    private final ListDataListener listDataListener = new ListDataListener() {
        @Override
        public void contentsChanged(ListDataEvent e) {
            if (myHoveredIndex >= e.getIndex0() && myHoveredIndex <= e.getIndex1()) {
                materialiseRendererAt(myHoveredIndex);
            }
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            if (myHoveredIndex > e.getIndex0() || myHoveredIndex > e.getIndex1()) {
                setHoveredIndex(-1);
            }
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            if (myHoveredIndex > e.getIndex0() || myHoveredIndex > e.getIndex1()) {
                setHoveredIndex(-1);
            }
        }
    };

    private final FocusListener focusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            materialiseRendererAt(myHoveredIndex);
        }

        @Override
        public void focusLost(FocusEvent e) {
            materialiseRendererAt(myHoveredIndex);
        }
    };

    private final ListSelectionListener selectionListener = e -> materialiseRendererAt(myHoveredIndex);

    private final ComponentAdapter componentListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            materialiseRendererAt(myHoveredIndex);
        }
    };

    private void materialiseRendererAt(int index) {
        if (index < 0 || index > myList.getModel().getSize() - 1) {
            setRendererComponent(null);
            return;
        }

        T cellValue = myList.getModel().getElementAt(index);
        boolean selected = myList.isSelectedIndex(index);
        boolean focused = myList.hasFocus() && selected;

        Component component = myCellRenderer.getListCellRendererComponent(myList, cellValue, index, selected, focused);
        if (resetCellBoundsOnHover) {
            component.setBounds(myList.getCellBounds(index, index));
        }
        setRendererComponent(component);
    }

    /**
     * Manually redraw the cell
     */
    public void update() {
        materialiseRendererAt(myHoveredIndex);
    }

    /**
     * {@code cellRenderer} should be an instance different from one in the list
     */
    public static <T> @Nonnull JListHoveredRowMaterialiser<T> install(
        @Nonnull JList<T> list,
        @Nonnull ListCellRenderer<T> cellRenderer
    ) {
        ListCellRenderer<T> listRenderer = list.getCellRenderer();
        if (listRenderer == cellRenderer ||
            (listRenderer instanceof ExpandedItemListCellRendererWrapper<?> wrapper && wrapper.getWrappee() == cellRenderer)) {
            throw new IllegalArgumentException("cellRenderer should be an instance different from list cell renderer");
        }

        JListHoveredRowMaterialiser<T> materialiser = new JListHoveredRowMaterialiser<>(list, cellRenderer);
        list.addMouseMotionListener(materialiser.listRowHoverListener);
        list.addFocusListener(materialiser.focusListener);
        list.addComponentListener(materialiser.componentListener);
        list.addListSelectionListener(materialiser.selectionListener);
        list.getModel().addListDataListener(materialiser.listDataListener);
        return materialiser;
    }
}
