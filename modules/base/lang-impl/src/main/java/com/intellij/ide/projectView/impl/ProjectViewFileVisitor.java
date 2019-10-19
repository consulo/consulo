// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.ui.tree.AbstractTreeNodeVisitor;
import javax.annotation.Nonnull;

import javax.swing.tree.TreePath;
import java.util.function.Predicate;

import static com.intellij.psi.util.PsiUtilCore.getVirtualFile;

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
