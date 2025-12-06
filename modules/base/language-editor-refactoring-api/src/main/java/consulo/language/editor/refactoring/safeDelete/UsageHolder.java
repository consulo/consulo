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
package consulo.language.editor.refactoring.safeDelete;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteReferenceUsageInfo;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.usage.UsageInfo;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author dsl
 */
class UsageHolder {
    private final SmartPsiElementPointer myElementPointer;
    private int myUnsafeUsages;
    private int myNonCodeUnsafeUsages;

    @RequiredReadAction
    public UsageHolder(PsiElement element, UsageInfo[] usageInfos) {
        Project project = element.getProject();
        myElementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element);

        for (UsageInfo usageInfo : usageInfos) {
            if (!(usageInfo instanceof SafeDeleteReferenceUsageInfo usage && usage.getReferencedElement() == element)) {
                continue;
            }

            if (!usage.isSafeDelete()) {
                myUnsafeUsages++;
                if (usage.isNonCodeUsage || isInGeneratedCode(usage, project)) {
                    myNonCodeUnsafeUsages++;
                }
            }
        }
    }

    @RequiredReadAction
    private static boolean isInGeneratedCode(SafeDeleteReferenceUsageInfo usage, Project project) {
        VirtualFile file = usage.getVirtualFile();
        return file != null && GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, project);
    }

    @Nonnull
    @RequiredReadAction
    public LocalizeValue getDescription() {
        PsiElement element = myElementPointer.getElement();
        LocalizeValue message = RefactoringLocalize.zeroHas1UsagesThatAreNotSafeToDelete(
            RefactoringUIUtil.getDescription(element, true),
            myUnsafeUsages
        );
        if (myNonCodeUnsafeUsages > 0) {
            message = LocalizeValue.join(
                message,
                LocalizeValue.of("<br>"),
                RefactoringLocalize.safeDeleteOfThose0InCommentsStringsNonCode(myNonCodeUnsafeUsages)
            );
        }
        return message;
    }

    public boolean hasUnsafeUsagesInCode() {
        return myUnsafeUsages != myNonCodeUnsafeUsages;
    }
}
