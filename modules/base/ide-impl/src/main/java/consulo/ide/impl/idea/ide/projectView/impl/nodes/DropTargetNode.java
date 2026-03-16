// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.dataContext.DataContext;
import consulo.language.psi.PsiFileSystemItem;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import static consulo.ui.ex.awt.tree.TreePathUtil.toTreeNodes;

/**
 * @author yole
 */
public interface DropTargetNode {
    boolean canDrop(TreeNode[] sourceNodes);

    default boolean canDrop(TreePath[] sources) {
        return canDrop(toTreeNodes(sources));
    }

    void drop(TreeNode[] sourceNodes, DataContext dataContext);

    default void drop(TreePath[] sources, DataContext dataContext) {
        drop(toTreeNodes(sources), dataContext);
    }

    void dropExternalFiles(PsiFileSystemItem[] sourceFileArray, DataContext dataContext);
}
