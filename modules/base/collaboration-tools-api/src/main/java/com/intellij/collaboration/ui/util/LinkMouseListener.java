// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.LinkMouseListenerBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

@ApiStatus.Internal
public final class LinkMouseListener<T> extends LinkMouseListenerBase<Object> {
    private final ClickableCellRenderer<T> myRenderer;

    public LinkMouseListener(@Nonnull ClickableCellRenderer<T> renderer) {
        myRenderer = renderer;
    }

    @Override
    protected @Nullable Object getTagAt(@Nonnull MouseEvent e) {
        @SuppressWarnings("unchecked")
        JBList<T> list = (JBList<T>) e.getSource();
        var model = list.getModel();

        int row = list.locationToIndex(e.getPoint());
        if (row < 0 || row >= model.getSize()) {
            return null;
        }

        myRenderer.getListCellRendererComponent(list, model.getElementAt(row), row, false, false);
        Rectangle rowBounds = list.getCellBounds(row, row);
        Point rowPoint = new Point(e.getPoint().x - rowBounds.x, e.getPoint().y - rowBounds.y);

        return myRenderer.getTagAt(rowPoint);
    }
}
