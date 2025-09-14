// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.AbstractTreeNodeVisitor;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreePath;
import java.util.function.Predicate;

import static consulo.language.psi.PsiUtilCore.getVirtualFile;
import static consulo.language.psi.SmartPointerManager.createPointer;

class ProjectViewNodeVisitor extends AbstractTreeNodeVisitor<PsiElement> {
  private final VirtualFile file;

  ProjectViewNodeVisitor(@Nonnull PsiElement element, @Nullable VirtualFile file, @Nullable Predicate<? super TreePath> predicate) {
    super(createPointer(element)::getElement, predicate);
    this.file = file;
    LOG.debug("create visitor for element: " + element);
  }

  /**
   * @return a virtual file corresponding to searching element or {@code null} if it is not set
   */
  @Nullable
  public final VirtualFile getFile() {
    return file;
  }

  @Override
  protected boolean contains(@Nonnull AbstractTreeNode node, @Nonnull PsiElement element) {
    return node instanceof ProjectViewNode && contains((ProjectViewNode)node, element) || super.contains(node, element);
  }

  private boolean contains(@Nonnull ProjectViewNode node, @Nonnull PsiElement element) {
    return contains(node, file) || contains(node, getVirtualFile(element));
  }

  private static boolean contains(@Nonnull ProjectViewNode node, VirtualFile file) {
    return file != null && node.contains(file);
  }

  @Override
  protected PsiElement getContent(@Nonnull AbstractTreeNode node) {
    Object value = node.getValue();
    return value instanceof PsiElement ? (PsiElement)value : null;
  }

  @Override
  protected boolean isAncestor(@Nonnull PsiElement content, @Nonnull PsiElement element) {
    return PsiTreeUtil.isAncestor(content, element, true);
  }
}
