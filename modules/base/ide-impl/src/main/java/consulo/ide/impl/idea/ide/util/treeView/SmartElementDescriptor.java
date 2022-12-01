/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.ide.impl.idea.ide.util.treeView;

import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.component.util.Iconable;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.PsiUtilCore;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SmartElementDescriptor extends NodeDescriptor {
  private final SmartPsiElementPointer mySmartPointer;
  private final Project myProject;

  public SmartElementDescriptor(@Nonnull Project project, NodeDescriptor parentDescriptor, @Nonnull PsiElement element) {
    super(parentDescriptor);
    myProject = project;
    mySmartPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element);
  }

  @Nullable
  public final PsiElement getPsiElement() {
    return mySmartPointer.getElement();
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  public Object getElement() {
    return getPsiElement();
  }

  protected boolean isMarkReadOnly() {
    return getParentDescriptor() instanceof PsiDirectoryNode;
  }

  protected boolean isMarkModified() {
    return getParentDescriptor() instanceof PsiDirectoryNode;
  }

  // Should be called in atomic action
  @RequiredUIAccess
  @Override
  public boolean update() {
    PsiElement element = mySmartPointer.getElement();
    if (element == null) return true;
    int flags = Iconable.ICON_FLAG_VISIBILITY;
    if (isMarkReadOnly()) {
      flags |= Iconable.ICON_FLAG_READ_STATUS;
    }
    Image icon = null;
    try {
      icon = IconDescriptorUpdaters.getIcon(element, flags);
    }
    catch (IndexNotReadyException ignored) {
    }
    ColorValue color = null;

    if (isMarkModified()) {
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
      if (virtualFile != null) {
        color = FileStatusManager.getInstance(myProject).getStatus(virtualFile).getColor();
      }
    }
    if (CopyPasteManager.getInstance().isCutElement(element)) {
      color = CopyPasteManager.CUT_COLOR;
    }

    boolean changes = !Comparing.equal(icon, getIcon()) || !Comparing.equal(color, myColor);
    setIcon(icon);
    myColor = color;
    return changes;
  }
}
