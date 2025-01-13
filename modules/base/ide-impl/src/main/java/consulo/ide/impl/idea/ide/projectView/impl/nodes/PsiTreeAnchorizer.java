/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.tree.TreeAnchorizer;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

/**
 * @author peter
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION)
public class PsiTreeAnchorizer extends TreeAnchorizer {
  @Nonnull
  @Override
  public Object createAnchor(@Nonnull Object element) {
    if (element instanceof PsiElement) {
      PsiElement psi = (PsiElement)element;
      return ReadAction.compute(() -> {
        if (!psi.isValid()) return psi;
        return SmartPointerManager.getInstance(psi.getProject()).createSmartPsiElementPointer(psi);
      });
    }
    return super.createAnchor(element);
  }

  @Override
  @Nullable
  public Object retrieveElement(@Nonnull final Object pointer) {
    if (pointer instanceof SmartPsiElementPointer) {
      return ReadAction.compute(() -> ((SmartPsiElementPointer)pointer).getElement());
    }

    return super.retrieveElement(pointer);
  }

  @Override
  public void freeAnchor(final Object element) {
    if (element instanceof SmartPsiElementPointer) {
      ApplicationManager.getApplication().runReadAction(() -> {
        SmartPsiElementPointer pointer = (SmartPsiElementPointer)element;
        Project project = pointer.getProject();
        if (!project.isDisposed()) {
          SmartPointerManager.getInstance(project).removePointer(pointer);
        }
      });
    }
  }
}
