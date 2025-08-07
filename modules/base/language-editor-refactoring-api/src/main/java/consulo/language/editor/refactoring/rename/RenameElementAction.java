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
package consulo.language.editor.refactoring.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.SyntheticElement;

import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "RenameElement")
public class RenameElementAction extends BaseRefactoringAction {
    public RenameElementAction() {
        super(ActionLocalize.actionRenameelementText(), ActionLocalize.actionRenameelementDescription());
        setInjectedContext(true);
    }

    @Override
    public boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        if (elements.length != 1) {
            return false;
        }

        PsiElement element = elements[0];
        return element instanceof PsiNamedElement && !(element instanceof SyntheticElement);
    }

    @Override
    @RequiredUIAccess
    public RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
        return RenameHandlerRegistry.getInstance().getRenameHandler(dataContext);
    }

    @Override
    @RequiredReadAction
    public boolean hasAvailableHandler(@Nonnull DataContext dataContext) {
        return isEnabledOnDataContext(dataContext);
    }

    @Override
    @RequiredReadAction
    protected boolean isEnabledOnDataContext(DataContext dataContext) {
        return RenameHandlerRegistry.getInstance().hasAvailableHandler(dataContext);
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
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
        return RenameHandlerRegistry.getInstance().hasAvailableHandler(context);
    }
}
