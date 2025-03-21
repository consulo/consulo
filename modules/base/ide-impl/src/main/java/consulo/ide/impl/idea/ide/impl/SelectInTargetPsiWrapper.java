/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.impl;

import consulo.application.dumb.IndexNotReadyException;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class SelectInTargetPsiWrapper implements SelectInTarget {
  protected final Project myProject;

  protected SelectInTargetPsiWrapper(@Nonnull final Project project) {
    myProject = project;
  }

  protected abstract boolean canSelect(PsiFileSystemItem file);

  @Override
  public final boolean canSelect(@Nonnull SelectInContext context) {
    if (!isContextValid(context)) return false;

    return canWorkWithCustomObjects() || canSelectInner(context);
  }

  protected boolean canSelectInner(@Nonnull SelectInContext context) {
    PsiFileSystemItem psiFile = getContextPsiFile(context);
    return psiFile != null && canSelect(psiFile);
  }

  private boolean isContextValid(SelectInContext context) {
    if (myProject.isDisposed()) return false;

    VirtualFile virtualFile = context.getVirtualFile();
    return virtualFile.isValid();
  }

  @Nullable
  protected PsiFileSystemItem getContextPsiFile(@Nonnull SelectInContext context) {
    VirtualFile virtualFile = context.getVirtualFile();
    PsiFileSystemItem psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);
    if (psiFile != null) {
      return psiFile;
    }

    if (context.getSelectorInFile() instanceof PsiFile) {
      return (PsiFile)context.getSelectorInFile();
    }
    if (virtualFile.isDirectory()) {
      return PsiManager.getInstance(myProject).findDirectory(virtualFile);
    }
    return null;
  }

  @Override
  public final void selectIn(@Nonnull SelectInContext context, boolean requestFocus) {
    VirtualFile file = context.getVirtualFile();
    Object selector = context.getSelectorInFile();
    if (selector == null) {
      PsiManager psiManager = PsiManager.getInstance(myProject);
      selector = file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
    }

    if (selector instanceof PsiElement) {
      select(((PsiElement)selector).getOriginalElement(), requestFocus);
    }
    else {
      select(selector, file, requestFocus);
    }
  }

  protected abstract void select(Object selector, VirtualFile virtualFile, boolean requestFocus);

  protected abstract boolean canWorkWithCustomObjects();

  protected abstract void select(PsiElement element, boolean requestFocus);

  @Nullable
  protected static PsiElement findElementToSelect(PsiElement element, PsiElement candidate) {
    PsiElement toSelect = candidate;

    if (toSelect == null) {
      if (element instanceof PsiFile || element instanceof PsiDirectory) {
        toSelect = element;
      }
      else {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile != null) {
          FileViewProvider viewProvider = containingFile.getViewProvider();
          toSelect = viewProvider.getPsi(viewProvider.getBaseLanguage());
        }
      }
    }

    if (toSelect != null) {
      PsiElement originalElement = null;
      try {
        originalElement = toSelect.getOriginalElement();
      }
      catch (IndexNotReadyException ignored) { }
      if (originalElement != null) {
        toSelect = originalElement;
      }
    }

    return toSelect;
  }
}