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

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreeModel;
import java.awt.Graphics;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopAWTTree extends DnDAwareTree implements FromSwingComponentWrapper {
    private final Component myOwner;

    public DesktopAWTTree(TreeModel model, Component owner) {
        super(model);
        myOwner = owner;
    }

    @Override
    public Component toUIComponent() {
        return myOwner;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isFileColorsEnabled()) {
            paintFileColorGutter(g);
        }

        super.paintComponent(g);
    }

    @Override
    public @Nullable ColorValue getFileColorFor(Object object) {
        if (object instanceof DesktopTreeNodeDescriptor<?> descriptor) {
            return descriptor.getPresentation().getBackground();
        }
        return null;
    }
}
