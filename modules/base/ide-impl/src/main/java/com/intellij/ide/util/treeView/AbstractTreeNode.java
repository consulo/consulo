// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.treeView;

import consulo.ui.ex.awt.CopyPasteManager;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.colorScheme.TextAttributesKey;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.tree.PresentationData;
import consulo.ui.ex.awt.tree.TreeNode;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusOwner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public abstract class AbstractTreeNode<T> extends TreeNode<T> implements FileStatusOwner {
  private static final TextAttributesKey FILESTATUS_ERRORS = TextAttributesKey.createTextAttributesKey("FILESTATUS_ERRORS");

  protected final Project myProject;

  protected AbstractTreeNode(Project project, @Nonnull T value) {
    super(value);
    myProject = project;
  }

  @Override
  @Nonnull
  @RequiredReadAction
  public abstract Collection<? extends AbstractTreeNode> getChildren();

  @Override
  protected boolean hasProblemFileBeneath() {
    return false;
  }

  protected boolean valueIsCut() {
    return CopyPasteManager.getInstance().isCutElement(getValue());
  }

  @Override
  protected void postprocess(@Nonnull PresentationData presentation) {
    if (hasProblemFileBeneath()) {
      presentation.setAttributesKey(FILESTATUS_ERRORS);
    }

    setForcedForeground(presentation);
  }

  @Override
  protected void setForcedForeground(@Nonnull PresentationData presentation) {
    final FileStatus status = getFileStatus();
    ColorValue fgColor = getFileStatusColor(status);
    fgColor = fgColor == null ? status.getColor() : fgColor;

    if (valueIsCut()) {
      fgColor = CopyPasteManager.CUT_COLOR;
    }

    if (presentation.getForcedTextForeground() == null) {
      presentation.setForcedTextForeground(fgColor);
    }
  }

  @Override
  protected boolean shouldUpdateData() {
    return !myProject.isDisposed() && getEqualityObject() != null;
  }

  public ColorValue getFileStatusColor(final FileStatus status) {
    if (FileStatus.NOT_CHANGED.equals(status) && !myProject.isDefault()) {
      final VirtualFile vf = getVirtualFile();
      if (vf != null && vf.isDirectory()) {
        return FileStatusManager.getInstance(myProject).getRecursiveStatus(vf).getColor();
      }
    }
    return status.getColor();
  }

  protected VirtualFile getVirtualFile() {
    return null;
  }

  @Override
  public FileStatus getFileStatus() {
    return FileStatus.NOT_CHANGED;
  }

  @Nullable
  public Project getProject() {
    return myProject;
  }
}
