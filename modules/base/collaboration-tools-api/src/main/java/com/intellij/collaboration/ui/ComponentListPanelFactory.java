// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.function.Function;

public final class ComponentListPanelFactory {
    private ComponentListPanelFactory() {
    }

    public static <T> @Nonnull JPanel createVertical(
        @Nonnull ListModel<T> model,
        int gap,
        @Nonnull Function<T, JComponent> componentFactory
    ) {
        JPanel panel = CollaborationToolsUIUtil.verticalListPanel(gap);

        model.addListDataListener(new ListDataListener() {
            @Override
            public void intervalRemoved(ListDataEvent e) {
                if (e.getIndex0() < 0 || e.getIndex1() < 0) {
                    return;
                }
                for (int i = e.getIndex1(); i >= e.getIndex0(); i--) {
                    panel.remove(i);
                }
                panel.revalidate();
                panel.repaint();
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                if (e.getIndex0() < 0 || e.getIndex1() < 0) {
                    return;
                }
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    panel.add(componentFactory.apply(model.getElementAt(i)), i);
                }
                panel.revalidate();
                panel.repaint();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                if (e.getIndex0() < 0 || e.getIndex1() < 0) {
                    return;
                }
                for (int i = e.getIndex1(); i >= e.getIndex0(); i--) {
                    panel.remove(i);
                }
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    panel.add(componentFactory.apply(model.getElementAt(i)), i);
                }
                panel.validate();
                panel.repaint();
            }
        });

        int size = model.getSize();
        for (int i = 0; i < size; i++) {
            panel.add(componentFactory.apply(model.getElementAt(i)));
        }

        return panel;
    }

    public static <T> @Nonnull JPanel createVertical(
        @Nonnull ListModel<T> model,
        @Nonnull Function<T, JComponent> componentFactory
    ) {
        return createVertical(model, 0, componentFactory);
    }
}
