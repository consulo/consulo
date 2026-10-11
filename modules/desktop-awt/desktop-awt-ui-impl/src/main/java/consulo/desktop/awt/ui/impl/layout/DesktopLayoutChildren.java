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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Consumer;

/**
 * Hands the children of a layout to a {@code forEachChild} consumer: a swing child only when it stands for a ui
 * component, a remembered ui component as it was given.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
final class DesktopLayoutChildren {
    private DesktopLayoutChildren() {
    }

    static java.awt.Component toAWT(Component component) {
        java.awt.Component awtComponent = TargetAWT.to(component);
        if (!(awtComponent instanceof FromSwingComponentWrapper) && awtComponent instanceof JComponent jComponent) {
            jComponent.putClientProperty(DesktopLayoutChildren.class, component);
        }
        return awtComponent;
    }

    static void accept(java.awt.@Nullable Component child, Consumer<Component> consumer) {
        if (child instanceof FromSwingComponentWrapper wrapper) {
            consumer.accept(wrapper.toUIComponent());
        }
        else if (child instanceof JComponent jComponent && jComponent.getClientProperty(DesktopLayoutChildren.class) instanceof Component added) {
            consumer.accept(added);
        }
    }

    static void acceptAll(Consumer<Component> consumer, @Nullable Component... components) {
        for (Component component : components) {
            if (component != null) {
                consumer.accept(component);
            }
        }
    }
}
