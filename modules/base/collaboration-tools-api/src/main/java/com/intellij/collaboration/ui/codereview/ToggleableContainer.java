// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

import com.intellij.collaboration.ui.SingleValueModel;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.ex.awt.BorderLayoutPanel;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * @see com.intellij.collaboration.ui.EditableComponentFactory
 * @deprecated Migrated to coroutines
 */
@Deprecated
public final class ToggleableContainer {
    private ToggleableContainer() {
    }

    public static @Nonnull JComponent create(
        @Nonnull SingleValueModel<Boolean> model,
        @Nonnull Supplier<JComponent> mainComponentSupplier,
        @Nonnull Supplier<JComponent> toggleableComponentSupplier
    ) {
        BorderLayoutPanel container = new BorderLayoutPanel();
        container.setOpaque(false);
        container.addToCenter(mainComponentSupplier.get());

        model.addListener(newValue -> {
            if (newValue) {
                updateToggleableContainer(container, toggleableComponentSupplier.get());
            }
            else {
                updateToggleableContainer(container, mainComponentSupplier.get());
            }
        });
        return container;
    }

    private static void updateToggleableContainer(@Nonnull BorderLayoutPanel container, @Nonnull JComponent component) {
        container.removeAll();
        container.addToCenter(component);
        container.revalidate();
        container.repaint();

        IdeFocusManager focusManager = IdeFocusManager.findInstanceByComponent(component);
        Component toFocus = focusManager.getFocusTargetFor(component);
        if (toFocus == null) {
            return;
        }
        focusManager.doWhenFocusSettlesDown(() -> focusManager.requestFocus(toFocus, true));
    }
}
