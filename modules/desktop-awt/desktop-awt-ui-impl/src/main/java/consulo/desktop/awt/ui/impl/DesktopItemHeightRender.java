/*
 * Copyright 2013-2026 consulo.io
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

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * JList has no per item height hook - with a fixed cell height of -1 it asks the renderer for each
 * row instead, so the height getter is applied to the rendered component's preferred size.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
final class DesktopLengthRender {
    static <E> ListCellRenderer<E> wrap(ListCellRenderer<E> delegate, Supplier<Function<E, Length>> getterSupplier) {
        return (list, value, index, selected, hasFocus) -> {
            Component component = delegate.getListCellRendererComponent(list, value, index, selected, hasFocus);

            Function<E, Length> getter = getterSupplier.get();
            if (getter != null && component != null) {
                Dimension size = component.getPreferredSize();
                component.setPreferredSize(new Dimension(size.width, DesktopLength.toPixels(list, getter.apply(value))));
            }

            return component;
        };
    }

    private DesktopLengthRender() {
    }
}
