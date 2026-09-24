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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.impl.internal.frame.UnifiedColoredTextContainer;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * A node of the {@link UnifiedXDebuggerTree} - the counterpart of the swing {@link XDebuggerTreeNode}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public abstract class UnifiedXDebuggerTreeNode {
    protected final UnifiedXDebuggerTree myTree;
    private final @Nullable UnifiedXDebuggerTreeNode myParent;

    /**
     * The row is drawn on the thread of the ui while the debug process changes the node - the text is built aside
     * and replaced as a whole.
     */
    private volatile UnifiedColoredTextContainer myText = new UnifiedColoredTextContainer();
    private volatile @Nullable Image myIcon;
    private volatile boolean myLeaf;

    private volatile @Nullable TreeNode<UnifiedXDebuggerTreeNode> myTreeNode;

    protected UnifiedXDebuggerTreeNode(UnifiedXDebuggerTree tree, @Nullable UnifiedXDebuggerTreeNode parent, boolean leaf) {
        myTree = tree;
        myParent = parent;
        myLeaf = leaf;
    }

    public UnifiedXDebuggerTree getTree() {
        return myTree;
    }

    public @Nullable UnifiedXDebuggerTreeNode getParent() {
        return myParent;
    }

    public boolean isLeaf() {
        return myLeaf;
    }

    protected void setLeaf(boolean leaf) {
        myLeaf = leaf;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
    }

    public UnifiedColoredTextContainer getText() {
        return myText;
    }

    protected void setText(UnifiedColoredTextContainer text) {
        myText = text;
    }

    protected void fireNodeChanged() {
        myTree.nodeChanged(this);
    }

    /**
     * What the node is found by again after the tree was rebuilt, {@code null} for a node which is not restored.
     */
    public @Nullable String getRestorableName() {
        return null;
    }

    public void render(TextItemPresentation presentation) {
        myText.appendTo(presentation);

        Image icon = myIcon;
        if (icon != null) {
            presentation.withIcon(icon);
        }
    }

    public @Nullable TreeNode<UnifiedXDebuggerTreeNode> getTreeNode() {
        return myTreeNode;
    }

    public void setTreeNode(TreeNode<UnifiedXDebuggerTreeNode> treeNode) {
        myTreeNode = treeNode;
    }
}
