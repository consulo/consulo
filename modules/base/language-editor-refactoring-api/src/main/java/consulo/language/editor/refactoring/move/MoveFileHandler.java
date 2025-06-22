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

package consulo.language.editor.refactoring.move;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.usage.UsageInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author Maxim.Mossienko
 * @since 2008-09-18
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class MoveFileHandler {
  public abstract boolean canProcessElement(PsiFile element);

  public abstract void prepareMovedFile(PsiFile file, PsiDirectory moveDestination, Map<PsiElement, PsiElement> oldToNewMap);

  @Nullable
  public abstract List<UsageInfo> findUsages(PsiFile psiFile, PsiDirectory newParent, boolean searchInComments, boolean searchInNonJavaFiles);

  public abstract void retargetUsages(List<UsageInfo> usageInfos, Map<PsiElement, PsiElement> oldToNewMap);

  public abstract void updateMovedFile(PsiFile file) throws IncorrectOperationException;

  @Nonnull
  public static MoveFileHandler forElement(PsiFile element) {
    for (MoveFileHandler processor : Application.get().getExtensionList(MoveFileHandler.class)) {
      if (processor.canProcessElement(element)) {
        return processor;
      }
    }
    return DEFAULT;
  }

  private static final MoveFileHandler DEFAULT = new MoveFileHandler() {
    @Override
    public boolean canProcessElement(final PsiFile element) {
      return true;
    }

    @Override
    public void prepareMovedFile(final PsiFile file, PsiDirectory moveDestination, Map<PsiElement, PsiElement> oldToNewMap) {

    }

    @Override
    public void updateMovedFile(final PsiFile file) throws IncorrectOperationException {

    }

    @Override
    public List<UsageInfo> findUsages(PsiFile psiFile, PsiDirectory newParent, boolean searchInComments, boolean searchInNonJavaFiles) {
      return null;
    }

    @Override
    public void retargetUsages(List<UsageInfo> usageInfos, Map<PsiElement, PsiElement> oldToNewMap) {

    }
  };


}
