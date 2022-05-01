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

package consulo.ide.impl.idea.refactoring.safeDelete;

import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.ide.impl.idea.refactoring.safeDelete.usageInfo.SafeDeleteReferenceUsageInfo;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.usage.UsageInfo;
import javax.annotation.Nonnull;

/**
 * @author dsl
 */
class UsageHolder {
  private final SmartPsiElementPointer myElementPointer;
  private int myUnsafeUsages;
  private int myNonCodeUnsafeUsages;

  public UsageHolder(PsiElement element, UsageInfo[] usageInfos) {
    Project project = element.getProject();
    myElementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element);

    GeneratedSourcesFilter[] filters = GeneratedSourcesFilter.EP_NAME.getExtensions();
    for (UsageInfo usageInfo : usageInfos) {
      if (!(usageInfo instanceof SafeDeleteReferenceUsageInfo)) continue;
      final SafeDeleteReferenceUsageInfo usage = (SafeDeleteReferenceUsageInfo)usageInfo;
      if (usage.getReferencedElement() != element) continue;

      if (!usage.isSafeDelete()) {
        myUnsafeUsages++;
        if (usage.isNonCodeUsage || isInGeneratedCode(usage, project, filters)) {
          myNonCodeUnsafeUsages++;
        }
      }
    }
  }

  private static boolean isInGeneratedCode(SafeDeleteReferenceUsageInfo usage, Project project, GeneratedSourcesFilter[] filters) {
    VirtualFile file = usage.getVirtualFile();
    if (file == null) return false;

    for (GeneratedSourcesFilter filter : filters) {
      if (filter.isGeneratedSource(file, project)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  public String getDescription() {
    final PsiElement element = myElementPointer.getElement();
    String message = RefactoringBundle.message("0.has.1.usages.that.are.not.safe.to.delete", RefactoringUIUtil.getDescription(element, true), myUnsafeUsages);
    if (myNonCodeUnsafeUsages > 0) {
      message += "<br>" + RefactoringBundle.message("safe.delete.of.those.0.in.comments.strings.non.code", myNonCodeUnsafeUsages);
    }
    return message;
  }

  public boolean hasUnsafeUsagesInCode() {
    return myUnsafeUsages != myNonCodeUnsafeUsages;
  }
}
