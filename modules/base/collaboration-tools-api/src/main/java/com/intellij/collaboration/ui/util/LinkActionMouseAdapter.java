// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ListUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class LinkActionMouseAdapter extends MouseAdapter {
    private final JList<?> myList;

    public LinkActionMouseAdapter(@Nonnull JList<?> list) {
        myList = list;
    }

    private @Nullable ActionListener getActionAt(@Nonnull MouseEvent e) {
        Point point = e.getPoint();
        Component renderer = ListUtil.getDeepestRendererChildComponentAt(myList, point);
        if (!(renderer instanceof SimpleColoredComponent scc)) {
            return null;
        }
        Object tag = scc.getFragmentTagAt(point.x);
        return tag instanceof ActionListener al ? al : null;
    }

    @Override
    public void mouseMoved(@Nonnull MouseEvent e) {
        ActionListener action = getActionAt(e);
        if (action != null) {
            UIUtil.setCursor(myList, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        else {
            UIUtil.setCursor(myList, Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mouseClicked(@Nonnull MouseEvent e) {
        ActionListener action = getActionAt(e);
        if (action != null) {
            action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "execute", e.getModifiersEx()));
        }
    }
}
