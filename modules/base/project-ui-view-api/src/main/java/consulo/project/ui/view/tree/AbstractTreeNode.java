// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.view.tree;

import consulo.fileEditor.VfsPresentationUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.colorScheme.TextAttributesKey;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.tree.TreeNode;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusOwner;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

public abstract class AbstractTreeNode<T> extends TreeNode<T> implements FileStatusOwner {
  private static final TextAttributesKey FILESTATUS_ERRORS = TextAttributesKey.createTextAttributesKey("FILESTATUS_ERRORS");

  protected final Project myProject;

  protected AbstractTreeNode(Project project, T value) {
    super(value);
    myProject = project;
  }

  @Override
  
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
  protected void postprocess(PresentationData presentation) {
    if (hasProblemFileBeneath()) {
      presentation.setAttributesKey(FILESTATUS_ERRORS);
    }

    setForcedForeground(presentation);
  }

  @Override
  protected void setForcedForeground(PresentationData presentation) {
    FileStatus status = getFileStatus();
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

  public ColorValue getFileStatusColor(FileStatus status) {
    if (FileStatus.NOT_CHANGED.equals(status) && !myProject.isDefault()) {
      VirtualFile vf = getVirtualFile();
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
  @RequiredReadAction
  protected @Nullable ColorValue computeBackgroundColor() {
    Object value = getValue();
    if (!(value instanceof PsiElement element)) {
      return null;
    }
    return getColorForElement(element);
  }

  public static @Nullable ColorValue getColorForElement(@Nullable PsiElement psi) {
    ColorValue color = null;
    if (psi != null) {
      if (!psi.isValid()) return null;

      Project project = psi.getProject();
      VirtualFile file = PsiUtilCore.getVirtualFile(psi);

      if (file != null) {
        color = VfsPresentationUtil.getFileBackgroundColor(project, file);
      }
      else if (psi instanceof PsiDirectory) {
        color = VfsPresentationUtil.getFileBackgroundColor(project, ((PsiDirectory)psi).getVirtualFile());
      }
      else if (psi instanceof PsiDirectoryContainer) {
        PsiDirectory[] dirs = ((PsiDirectoryContainer)psi).getDirectories();
        for (PsiDirectory dir : dirs) {
          ColorValue c = VfsPresentationUtil.getFileBackgroundColor(project, dir.getVirtualFile());
          if (c != null && color == null) {
            color = c;
          }
          else if (c != null) {
            color = null;
            break;
          }
        }
      }
    }
    return color;
  }

  @Override
  public FileStatus getFileStatus() {
    return FileStatus.NOT_CHANGED;
  }

  public @Nullable Project getProject() {
    return myProject;
  }
}
