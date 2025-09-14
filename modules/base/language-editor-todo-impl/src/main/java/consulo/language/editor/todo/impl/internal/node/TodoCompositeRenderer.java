// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.todo.impl.internal.node;

import consulo.ui.ex.awt.HighlightableCellRenderer;
import consulo.ui.ex.awt.HighlightedRegion;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * todo: replace this highlightable crap with regular NodeRenderer
 *
 * @author Vladimir Kondratyev
 */
public final class TodoCompositeRenderer implements TreeCellRenderer {
    private final NodeRenderer myNodeRenderer;
    private final HighlightableCellRenderer myColorTreeCellRenderer;
    private final MultiLineTodoRenderer myMultiLineRenderer;

    public TodoCompositeRenderer() {
        myNodeRenderer = new NodeRenderer();
        myColorTreeCellRenderer = new HighlightableCellRenderer();
        myMultiLineRenderer = new MultiLineTodoRenderer();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component result;

        Object userObject = ((DefaultMutableTreeNode) obj).getUserObject();
        if (userObject instanceof SummaryNode) {
            myNodeRenderer.getTreeCellRendererComponent(tree, userObject.toString(), selected, expanded, leaf, row, hasFocus);
            myNodeRenderer.setFont(UIUtil.getTreeFont().deriveFont(Font.BOLD));
            myNodeRenderer.setIcon((Image) null);
            result = myNodeRenderer;
        }
        else if (userObject instanceof TodoItemNode && !((TodoItemNode) userObject).getAdditionalLines().isEmpty()) {
            myMultiLineRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
            result = myMultiLineRenderer;
        }
        else if (userObject instanceof NodeDescriptor && userObject instanceof HighlightedRegionProvider) {
            NodeDescriptor descriptor = (NodeDescriptor) userObject;
            HighlightedRegionProvider regionProvider = (HighlightedRegionProvider) userObject;
            myColorTreeCellRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
            for (HighlightedRegion region : regionProvider.getHighlightedRegions()) {
                myColorTreeCellRenderer.addHighlighter(region.startOffset, region.endOffset, region.textAttributes);
            }
            myColorTreeCellRenderer.setIcon(descriptor.getIcon());
            result = myColorTreeCellRenderer;
        }
        else {
            result = myNodeRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
        }

        if (result instanceof JComponent) {
            ((JComponent) result).setOpaque(!selected);
        }

        return result;
    }
}
