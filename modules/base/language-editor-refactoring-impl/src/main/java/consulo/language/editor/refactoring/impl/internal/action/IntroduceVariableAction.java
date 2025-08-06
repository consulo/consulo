/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.action.BasePlatformRefactoringAction;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.psi.PsiElement;
import consulo.platform.base.localize.ActionLocalize;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "IntroduceVariable")
public class IntroduceVariableAction extends BasePlatformRefactoringAction {
    public IntroduceVariableAction() {
        super(ActionLocalize.actionIntroducevariableText(), ActionLocalize.actionIntroducevariableDescription());
        setInjectedContext(true);
    }

    @Override
    protected boolean isAvailableInEditorOnly() {
        return true;
    }

    @Override
    @RequiredReadAction
    protected boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        return false;
    }

    @Override
    protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
        return provider.getIntroduceVariableHandler();
    }
}
