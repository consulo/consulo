/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.action.toolbar;

import consulo.ui.ex.action.ActionButton;
import consulo.ui.ex.internal.ActionToolbarEx;

import java.awt.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2024-12-31
 */
public interface DesktopAWTActionToolbar extends ActionToolbarEx {
    String SUPPRESS_TARGET_COMPONENT_WARNING = "ActionToolbarImpl.suppressTargetComponentWarning";

    Style getStyle();

    int getHeight();

    int getWidth();

    int getComponentCount();

    Component getComponent(int n);

    @Override
    default void forEachButton(Consumer<ActionButton> buttonConsumer) {
        for (int i = 0; i < getComponentCount(); i++) {
            Component component = getComponent(i);

            if (component instanceof ActionButton actionButton) {
                buttonConsumer.accept(actionButton);
            }
        }
    }

    default Dimension getChildPreferredSize(int index) {
        Component component = getComponent(index);
        return component.isVisible() ? component.getPreferredSize() : new Dimension();
    }

    /**
     * @return maximum button width
     */
    default int getMaxButtonWidth() {
        int width = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            final Dimension dimension = getChildPreferredSize(i);
            width = Math.max(width, dimension.width);
        }
        return width;
    }

    default int getMaxButtonHeight() {
        int height = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            final Dimension dimension = getChildPreferredSize(i);
            height = Math.max(height, dimension.height);
        }
        return height;
    }
}
