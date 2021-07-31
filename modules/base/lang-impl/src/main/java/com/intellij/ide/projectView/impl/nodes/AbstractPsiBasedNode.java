// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.ValidateableNode;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.StatePreservingNavigatable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.AstLoadingFilter;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdaters;
import consulo.logging.Logger;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Class for node descriptors based on PsiElements. Subclasses should define
 * method that extract PsiElement from Value.
 *
 * @param <Value> Value of node descriptor
 */
public abstract class AbstractPsiBasedNode<Value> extends ProjectViewNode<Value> implements ValidateableNode, StatePreservingNavigatable {
  private static final Logger LOG = Logger.getInstance(AbstractPsiBasedNode.class);

  protected AbstractPsiBasedNode(final Project project, @Nonnull Value value, final ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Nullable
  protected abstract PsiElement extractPsiFromValue();

  @Nullable
  protected abstract Collection<AbstractTreeNode> getChildrenImpl();

  protected abstract void updateImpl(@Nonnull PresentationData data);

  @Override
  @Nonnull
  public final Collection<? extends AbstractTreeNode> getChildren() {
    return AstLoadingFilter.disallowTreeLoading(this::doGetChildren);
  }

  @Nonnull
  private Collection<? extends AbstractTreeNode> doGetChildren() {
    final PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null) {
      return new ArrayList<>();
    }
    if (!psiElement.isValid()) {
      LOG.error(new IllegalStateException("Node contains invalid PSI: " + "\n" + getClass() + " [" + this + "]" + "\n" + psiElement.getClass() + " [" + psiElement + "]"));
      return Collections.emptyList();
    }

    final Collection<AbstractTreeNode> children = getChildrenImpl();
    return children != null ? children : Collections.emptyList();
  }

  @Override
  public boolean isValid() {
    final PsiElement psiElement = extractPsiFromValue();
    return psiElement != null && psiElement.isValid();
  }

  protected boolean isMarkReadOnly() {
    final AbstractTreeNode<?> parent = getParent();
    if (parent == null) {
      return false;
    }
    if (parent instanceof AbstractPsiBasedNode) {
      final PsiElement psiElement = ((AbstractPsiBasedNode<?>)parent).extractPsiFromValue();
      return psiElement instanceof PsiDirectory;
    }

    final Object parentValue = parent.getValue();
    return parentValue instanceof PsiDirectory || parentValue instanceof Module;
  }

  @Override
  public FileStatus getFileStatus() {
    return computeFileStatus(getVirtualFileForValue(), Objects.requireNonNull(getProject()));
  }

  protected static FileStatus computeFileStatus(@Nullable VirtualFile virtualFile, @Nonnull Project project) {
    if (virtualFile == null) {
      return FileStatus.NOT_CHANGED;
    }
    return FileStatusManager.getInstance(project).getStatus(virtualFile);
  }

  @Nullable
  private VirtualFile getVirtualFileForValue() {
    PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null) {
      return null;
    }
    return PsiUtilCore.getVirtualFile(psiElement);
  }

  // Should be called in atomic action

  @Override
  public void update(@Nonnull final PresentationData data) {
    AstLoadingFilter.disallowTreeLoading(() -> doUpdate(data));
  }

  private void doUpdate(@Nonnull PresentationData data) {
    ApplicationManager.getApplication().runReadAction(() -> {
      if (!validate()) {
        return;
      }

      final PsiElement value = extractPsiFromValue();
      LOG.assertTrue(value.isValid());

      int flags = getIconableFlags();

      try {
        Image icon = IconDescriptorUpdaters.getIcon(value, flags);
        data.setIcon(icon);
      }
      catch (IndexNotReadyException ignored) {
      }
      data.setPresentableText(myName);

      try {
        if (isDeprecated()) {
          data.setAttributesKey(CodeInsightColors.DEPRECATED_ATTRIBUTES);
        }
      }
      catch (IndexNotReadyException ignored) {
      }

      updateImpl(data);
      data.setIcon(patchIcon(myProject, data.getIcon(), getVirtualFile()));

      for (ProjectViewNodeDecorator decorator : ProjectViewNodeDecorator.EP_NAME.getExtensionList(myProject)) {
        decorator.decorate(AbstractPsiBasedNode.this, data);
      }
    });
  }

  @Nullable
  public static Image patchIcon(@Nonnull Project project, @Nullable Image original, @Nullable VirtualFile file) {
    if (file == null || original == null) return original;

    IconDescriptor iconDescriptor = new IconDescriptor(original);

    final Bookmark bookmarkAtFile = BookmarkManager.getInstance(project).findFileBookmark(file);
    if (bookmarkAtFile != null) {
      iconDescriptor.setRightIcon(bookmarkAtFile.getIcon(false));
    }

    if (!file.isWritable()) {
      iconDescriptor.addLayerIcon(AllIcons.Nodes.Locked);
    }

    if (file.is(VFileProperty.SYMLINK)) {
      iconDescriptor.addLayerIcon(AllIcons.Nodes.Symlink);
    }

    return iconDescriptor.toIcon();
  }

  @Iconable.IconFlags
  protected int getIconableFlags() {
    int flags = 0;
    if (isMarkReadOnly()) {
      flags |= Iconable.ICON_FLAG_READ_STATUS;
    }
    return flags;
  }

  protected boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean contains(@Nonnull final VirtualFile file) {
    final PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null || !psiElement.isValid()) {
      return false;
    }

    final PsiFile containingFile = psiElement.getContainingFile();
    if (containingFile == null) {
      return false;
    }
    final VirtualFile valueFile = containingFile.getVirtualFile();
    return file.equals(valueFile);
  }

  @Nullable
  public NavigationItem getNavigationItem() {
    final PsiElement psiElement = extractPsiFromValue();
    return psiElement instanceof NavigationItem ? (NavigationItem)psiElement : null;
  }

  @Override
  public void navigate(boolean requestFocus, boolean preserveState) {
    if (canNavigate()) {
      if (requestFocus || preserveState) {
        NavigationUtil.openFileWithPsiElement(extractPsiFromValue(), requestFocus, requestFocus);
      }
      else {
        getNavigationItem().navigate(requestFocus);
      }
    }
  }

  @Override
  public void navigate(boolean requestFocus) {
    navigate(requestFocus, false);
  }

  @Override
  public boolean canNavigate() {
    final NavigationItem item = getNavigationItem();
    return item != null && item.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    final NavigationItem item = getNavigationItem();
    return item != null && item.canNavigateToSource();
  }

  @Nullable
  protected String calcTooltip() {
    return null;
  }

  @Override
  public boolean validate() {
    final PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null || !psiElement.isValid()) {
      setValue(null);
    }

    return getValue() != null;
  }
}
