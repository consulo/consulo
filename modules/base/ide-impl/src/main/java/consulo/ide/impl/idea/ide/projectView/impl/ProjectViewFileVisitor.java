// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.idea.ui.tree.AbstractTreeNodeVisitor;
import jakarta.annotation.Nonnull;

import javax.swing.tree.TreePath;
import java.util.function.Predicate;

import static consulo.language.psi.PsiUtilCore.getVirtualFile;

class ProjectViewFileVisitor extends AbstractTreeNodeVisitor<VirtualFile> {
  ProjectViewFileVisitor(@Nonnull VirtualFile file, Predicate<? super TreePath> predicate) {
    super(() -> file, predicate);
    LOG.debug("create visitor for file: " + file);
  }

  @Override
  protected boolean contains(@Nonnull AbstractTreeNode node, @Nonnull VirtualFile file) {
    return node instanceof ProjectViewNode && contains((ProjectViewNode)node, file) || super.contains(node, file);
  }

  private static boolean contains(@Nonnull ProjectViewNode node, @Nonnull VirtualFile file) {
    return node.contains(file);
  }

  @Override
  protected VirtualFile getContent(@Nonnull AbstractTreeNode node) {
    Object value = node.getValue();
    return value instanceof PsiElement ? getVirtualFile((PsiElement)value) : null;
  }

  @Override
  protected boolean isAncestor(@Nonnull VirtualFile content, @Nonnull VirtualFile file) {
    return VfsUtilCore.isAncestor(content, file, true);
  }
}
