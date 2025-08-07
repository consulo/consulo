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
package consulo.language.editor.refactoring.impl.internal.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteHandler;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteProcessor;
import consulo.platform.base.localize.ActionLocalize;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "SafeDelete")
public class SafeDeleteAction extends BaseRefactoringAction {
    public SafeDeleteAction() {
        super(ActionLocalize.actionSafedeleteText(), ActionLocalize.actionSafedeleteDescription());
        setInjectedContext(true);
    }

    @Override
    public boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        return true;
    }

    @Override
    @RequiredReadAction
    public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        for (PsiElement element : elements) {
            if (!SafeDeleteProcessor.validElement(element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @RequiredReadAction
    protected boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    ) {
        return SafeDeleteProcessor.validElement(element);
    }

    @Override
    public RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
        return new SafeDeleteHandler();
    }
}
