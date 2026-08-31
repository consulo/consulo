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

package consulo.language.editor.refactoring.inline;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

public abstract class InlineOptionsWithSearchSettingsDialog extends InlineOptionsDialog {
    protected CheckBox myCbSearchInComments;
    protected CheckBox myCbSearchTextOccurrences;

    protected InlineOptionsWithSearchSettingsDialog(Project project, boolean canBeParent, PsiElement element) {
        super(project, canBeParent, element);
    }

    protected abstract boolean isSearchInCommentsAndStrings();

    protected abstract void saveSearchInCommentsAndStrings(boolean searchInComments);

    protected abstract boolean isSearchForTextOccurrences();

    protected abstract void saveSearchInTextOccurrences(boolean searchInTextOccurrences);

    @Override
    protected void doAction() {
        boolean searchInNonJava = myCbSearchTextOccurrences.getValueOrError();
        boolean searchInComments = myCbSearchInComments.getValueOrError();
        if (myCbSearchInComments.isEnabled()) {
            saveSearchInCommentsAndStrings(searchInComments);
        }
        if (myCbSearchTextOccurrences.isEnabled()) {
            saveSearchInTextOccurrences(searchInNonJava);
        }
    }

    @RequiredUIAccess
    public void setEnabledSearchSettings(boolean enabled) {
        myCbSearchInComments.setEnabled(enabled);
        myCbSearchTextOccurrences.setEnabled(enabled);
        if (enabled) {
            myCbSearchInComments.setValue(isSearchInCommentsAndStrings());
            myCbSearchTextOccurrences.setValue(isSearchForTextOccurrences());
        }
        else {
            myCbSearchInComments.setValue(false);
            myCbSearchTextOccurrences.setValue(false);
        }
    }

    @RequiredUIAccess
    @Override
    protected VerticalLayout createCenterUIComponent() {
        VerticalLayout layout = super.createCenterUIComponent();

        myCbSearchInComments = CheckBox.create(RefactoringLocalize.searchInCommentsAndStrings(), isSearchInCommentsAndStrings());
        myCbSearchTextOccurrences = CheckBox.create(RefactoringLocalize.searchForTextOccurrences(), isSearchForTextOccurrences());

        ComponentEventListener<ValueComponent<Boolean>, ValueComponentEvent<Boolean>> actionListener = e ->
            setEnabledSearchSettings(myRbInlineAll.getValueOrError());

        myRbInlineThisOnly.addValueListener(actionListener);
        myRbInlineAll.addValueListener(actionListener);

        setEnabledSearchSettings(myRbInlineAll.getValueOrError());

        layout.add(HorizontalLayout.create().add(myCbSearchInComments).add(myCbSearchTextOccurrences));
        return layout;
    }
}
