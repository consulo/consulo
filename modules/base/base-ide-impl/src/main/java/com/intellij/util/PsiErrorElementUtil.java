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
package com.intellij.util;

import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import com.intellij.psi.impl.PsiManagerEx;
import consulo.language.psi.util.CachedValue;
import consulo.language.psi.util.CachedValueProvider;
import consulo.language.psi.util.CachedValuesManager;
import javax.annotation.Nonnull;

public class PsiErrorElementUtil {

  private static final Key<CachedValue<Boolean>> CONTAINS_ERROR_ELEMENT = Key.create("CONTAINS_ERROR_ELEMENT");

  private PsiErrorElementUtil() {
  }

  public static boolean hasErrors(@Nonnull final Project project, @Nonnull final VirtualFile virtualFile) {
    return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
      @Override
      public Boolean compute() {
        if (project.isDisposed() || !virtualFile.isValid()) {
          return false;
        }
        PsiManagerEx psiManager = PsiManagerEx.getInstanceEx(project);
        PsiFile psiFile = psiManager.getFileManager().findFile(virtualFile);
        return psiFile != null && hasErrors(psiFile);
      }
    });
  }

  private static boolean hasErrors(@Nonnull final PsiFile psiFile) {
    CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(psiFile.getProject());
    return cachedValuesManager.getCachedValue(psiFile, CONTAINS_ERROR_ELEMENT, () -> {
      boolean error = hasErrorElements(psiFile);
      return CachedValueProvider.Result.create(error, psiFile);
    }, false);
  }

  private static boolean hasErrorElements(@Nonnull final PsiElement element) {
    if (element instanceof PsiErrorElement) {
      for (HighlightErrorFilter errorFilter : HighlightErrorFilter.EP_NAME.getExtensionList(element.getProject())) {
        if (!errorFilter.shouldHighlightErrorElement((PsiErrorElement)element)) {
          return false;
        }
      }
      return true;
    }
    for (PsiElement child : element.getChildren()) {
      if (hasErrorElements(child)) {
        return true;
      }
    }
    return false;
  }
}
