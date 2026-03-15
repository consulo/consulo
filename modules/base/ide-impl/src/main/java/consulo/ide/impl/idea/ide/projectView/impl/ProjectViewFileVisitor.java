// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.language.psi.PsiElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.AbstractTreeNodeVisitor;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.swing.tree.TreePath;
import java.util.function.Predicate;

import static consulo.language.psi.PsiUtilCore.getVirtualFile;

class ProjectViewFileVisitor extends AbstractTreeNodeVisitor<VirtualFile> {
  ProjectViewFileVisitor(VirtualFile file, Predicate<? super TreePath> predicate) {
    super(() -> file, predicate);
    LOG.debug("create visitor for file: " + file);
  }

  @Override
  protected boolean contains(AbstractTreeNode node, VirtualFile file) {
    return node instanceof ProjectViewNode && contains((ProjectViewNode)node, file) || super.contains(node, file);
  }

  private static boolean contains(ProjectViewNode node, VirtualFile file) {
    return node.contains(file);
  }

  @Override
  protected VirtualFile getContent(AbstractTreeNode node) {
    Object value = node.getValue();
    return value instanceof PsiElement ? getVirtualFile((PsiElement)value) : null;
  }

  @Override
  protected boolean isAncestor(VirtualFile content, VirtualFile file) {
    return VirtualFileUtil.isAncestor(content, file, true);
  }
}
