/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.codeEditor.Editor;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author yole
 */
public abstract class RenameWithOptionalReferencesDialog extends RenameDialog {
    private @Nullable CheckBox myCbSearchForReferences;

    @RequiredUIAccess
    public RenameWithOptionalReferencesDialog(
        Project project,
        PsiElement psiElement,
        @Nullable PsiElement nameSuggestionContext,
        @Nullable Editor editor
    ) {
        super(project, psiElement, nameSuggestionContext, editor);
    }

    @Override
    @RequiredUIAccess
    protected void createCheckboxes(List<Component> options) {
        CheckBox searchForReferences = CheckBox.create(RefactoringLocalize.searchForReferences(), getSearchForReferences());
        searchForReferences.setId("searchForReferences");
        myCbSearchForReferences = searchForReferences;
        options.add(searchForReferences);

        super.createCheckboxes(options);
    }

    @Override
    @RequiredUIAccess
    protected void doAction() {
        CheckBox searchForReferences = myCbSearchForReferences;
        if (searchForReferences != null) {
            setSearchForReferences(Boolean.TRUE.equals(searchForReferences.getValue()));
        }
        super.doAction();
    }

    protected abstract boolean getSearchForReferences();

    protected abstract void setSearchForReferences(boolean value);
}
