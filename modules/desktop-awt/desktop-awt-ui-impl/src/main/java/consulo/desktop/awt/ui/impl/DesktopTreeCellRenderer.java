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

import consulo.ui.ex.awt.CellRendererPanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.font.Font;
import org.jspecify.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
class DesktopTreeCellRenderer extends CellRendererPanel implements TreeCellRenderer {
    private final NodeRenderer myTextRenderer = new NodeRenderer();
    private final DesktopTextItemSuffixComponent mySuffixComponent = new DesktopTextItemSuffixComponent();

    private @Nullable Font myTextFont;

    DesktopTreeCellRenderer() {
        super(new BorderLayout());

        add(myTextRenderer, BorderLayout.CENTER);
        add(mySuffixComponent, BorderLayout.EAST);
    }

    @Override
    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        setPreferredSize(null);

        DesktopTreeRowDecoration decoration = decorationOf(value);

        Font font = decoration.font();
        if (!Objects.equals(font, myTextFont)) {
            myTextFont = font;
            myTextRenderer.setFont(font == null ? null : TargetAWT.to(font));
        }

        myTextRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        mySuffixComponent.update(
            decoration.suffixText(),
            decoration.suffixIcon(),
            selected && hasFocus ? UIUtil.getTreeSelectionForeground(true) : null,
            tree.getFont()
        );

        return this;
    }

    private static DesktopTreeRowDecoration decorationOf(Object value) {
        Object userObject = value instanceof DesktopTreeNodeDescriptor ? value : TreeUtil.getUserObject(value);
        if (userObject instanceof DesktopTreeNodeDescriptor<?> descriptor) {
            return descriptor.getDecoration();
        }
        return DesktopTreeRowDecoration.EMPTY;
    }

    @Override
    public @Nullable String getToolTipText() {
        return myTextRenderer.getToolTipText();
    }

    @Override
    public @Nullable String getToolTipText(MouseEvent event) {
        return myTextRenderer.getToolTipText();
    }
}
