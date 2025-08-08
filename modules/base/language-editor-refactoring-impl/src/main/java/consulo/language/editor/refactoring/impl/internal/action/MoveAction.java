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

import consulo.annotation.component.ActionImpl;
import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.move.MoveHandler;
import consulo.platform.base.localize.ActionLocalize;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Move")
public class MoveAction extends BaseRefactoringAction {
    public MoveAction() {
        super(ActionLocalize.actionMoveText(), ActionLocalize.actionMoveDescription());
        setInjectedContext(true);
    }

    @Override
    public boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        // move is supported in any language
        return true;
    }

    @Override
    public boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        return MoveHandler.canMove(elements, null);
    }

    @Override
    protected boolean isEnabledOnDataContext(DataContext dataContext) {
        return MoveHandler.canMove(dataContext);
    }

    @Override
    public RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
        return new MoveHandler();
    }
}