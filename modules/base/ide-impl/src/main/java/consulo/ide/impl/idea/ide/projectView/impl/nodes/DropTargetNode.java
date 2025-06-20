// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.dataContext.DataContext;
import consulo.language.psi.PsiFileSystemItem;
import jakarta.annotation.Nonnull;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import static consulo.ui.ex.awt.tree.TreePathUtil.toTreeNodes;

/**
 * @author yole
 */
public interface DropTargetNode {
    boolean canDrop(@Nonnull TreeNode[] sourceNodes);

    default boolean canDrop(@Nonnull TreePath[] sources) {
        return canDrop(toTreeNodes(sources));
    }

    void drop(@Nonnull TreeNode[] sourceNodes, @Nonnull DataContext dataContext);

    default void drop(@Nonnull TreePath[] sources, @Nonnull DataContext dataContext) {
        drop(toTreeNodes(sources), dataContext);
    }

    void dropExternalFiles(PsiFileSystemItem[] sourceFileArray, DataContext dataContext);
}
