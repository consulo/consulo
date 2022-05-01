// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.dnd;

import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.tree.TreePathUtil;
import javax.annotation.Nullable;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * @author Konstantin Bulenkov
 */
public interface TransferableWrapper extends FileFlavorProvider {
  @Nullable
  default TreePath[] getTreePaths() {
    return TreePathUtil.toTreePaths(getTreeNodes());
  }

  @Nullable
  TreeNode[] getTreeNodes();

  @Nullable
  PsiElement[] getPsiElements();
}
