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
package consulo.language.impl.internal.psi.pointer;

import consulo.language.Language;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.document.util.ProperTextRange;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import jakarta.annotation.Nonnull;

class SmartPsiFileRangePointerImpl extends SmartPsiElementPointerImpl<PsiFile> implements SmartPsiFileRange {
  SmartPsiFileRangePointerImpl(@Nonnull SmartPointerManagerImpl manager, @Nonnull PsiFile containingFile, @Nonnull ProperTextRange range, boolean forInjected) {
    super(manager, containingFile, createElementInfo(containingFile, range, forInjected));
  }

  @Nonnull
  private static SmartPointerElementInfo createElementInfo(@Nonnull PsiFile containingFile, @Nonnull ProperTextRange range, boolean forInjected) {
    Project project = containingFile.getProject();
    if (containingFile.getViewProvider() instanceof FreeThreadedFileViewProvider) {
      PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(project).getInjectionHost(containingFile);
      if (host != null) {
        SmartPsiElementPointer<PsiLanguageInjectionHost> hostPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(host);
        return new InjectedSelfElementInfo(project, containingFile, range, containingFile, hostPointer);
      }
    }
    if (!forInjected && range.equals(containingFile.getTextRange())) return new FileElementInfo(containingFile);
    return new SelfElementInfo(range, IdentikitImpl.fromTypes(PsiElement.class, null, Language.ANY), containingFile, forInjected);
  }

  @Override
  public PsiFile getContainingFile() {
    return getElementInfo().restoreFile(myManager);
  }

  @Override
  public PsiFile getElement() {
    if (getRange() == null) return null; // range is invalid
    return getContainingFile();
  }

  @Override
  public String toString() {
    return "SmartPsiFileRangePointerImpl{" + getElementInfo() + "}";
  }
}
