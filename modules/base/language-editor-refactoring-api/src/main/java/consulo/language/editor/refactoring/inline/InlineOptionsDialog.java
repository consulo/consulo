/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.ui.util.RadioUpDownListener;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.ReferencesSearch;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.ValueGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public abstract class InlineOptionsDialog extends RefactoringDialog implements InlineOptions {
    protected RadioButton myRbInlineAll;
    protected RadioButton myRbInlineThisOnly;
    protected boolean myInvokedOnReference;
    protected final PsiElement myElement;
    private Label myNameLabel;

    protected InlineOptionsDialog(Project project, boolean canBeParent, PsiElement element) {
        super(project, canBeParent);
        myElement = element;
    }

    @Override
    protected JComponent createNorthPanel() {
        if (myNameLabel == null) {
            myNameLabel = Label.create(getNameLabelText());
        }
        return (JComponent) TargetAWT.to(myNameLabel);
    }

    @Override
    public boolean isInlineThisOnly() {
        return myRbInlineThisOnly.getValueOrError();
    }

    @Override
    @RequiredUIAccess
    protected JComponent createCenterPanel() {
        return (JComponent) TargetAWT.to(createCenterUIComponent());
    }

    @RequiredUIAccess
    protected VerticalLayout createCenterUIComponent() {
        VerticalLayout layout = VerticalLayout.create();

        myRbInlineAll = RadioButton.create(getInlineAllText());
        myRbInlineAll.setValue(true);
        myRbInlineThisOnly = RadioButton.create(getInlineThisText());

        layout.add(myRbInlineAll);
        layout.add(myRbInlineThisOnly);
        ValueGroup<Boolean> bg = ValueGroup.createBool();
        bg.add(myRbInlineAll);
        bg.add(myRbInlineThisOnly);
        RadioUpDownListener.registerListener(myRbInlineAll, myRbInlineThisOnly);

        myRbInlineThisOnly.setEnabled(myInvokedOnReference);
        final boolean writable = myElement.isWritable();
        myRbInlineAll.setEnabled(writable);
        if (myInvokedOnReference) {
            if (canInlineThisOnly()) {
                myRbInlineAll.setValue(false);
                myRbInlineAll.setEnabled(false);
                myRbInlineThisOnly.setValue(true);
            }
            else {
                if (writable) {
                    final boolean inlineThis = isInlineThis();
                    myRbInlineThisOnly.setValue(inlineThis);
                    myRbInlineAll.setValue(!inlineThis);
                }
                else {
                    myRbInlineAll.setValue(false);
                    myRbInlineThisOnly.setValue(true);
                }
            }
        }
        else {
            myRbInlineAll.setValue(true);
            myRbInlineThisOnly.setValue(false);
        }

        getPreviewAction().setEnabled(myRbInlineAll.getValueOrError());
        myRbInlineAll.addValueListener(event -> {
            boolean enabled = myRbInlineAll.getValueOrError();
            getPreviewAction().setEnabled(enabled);
        });
        return layout;
    }

    @Nonnull
    protected abstract LocalizeValue getNameLabelText();

    @Nonnull
    protected abstract LocalizeValue getBorderTitle();

    @Nonnull
    protected abstract LocalizeValue getInlineAllText();

    @Nonnull
    protected abstract LocalizeValue getInlineThisText();

    protected abstract boolean isInlineThis();

    protected boolean canInlineThisOnly() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return (JComponent) TargetAWT.to(myRbInlineThisOnly.getValue() ? myRbInlineThisOnly : myRbInlineAll);
    }

    @RequiredReadAction
    protected static int initOccurrencesNumber(PsiNameIdentifierOwner nameIdentifierOwner) {
        final ProgressManager progressManager = ProgressManager.getInstance();
        final PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(nameIdentifierOwner.getProject());
        final GlobalSearchScope scope = GlobalSearchScope.projectScope(nameIdentifierOwner.getProject());
        final String name = nameIdentifierOwner.getName();
        final boolean isCheapToSearch =
            name != null && searchHelper.isCheapEnoughToSearch(name, scope, null, progressManager.getProgressIndicator()) != PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES;
        return isCheapToSearch ? ReferencesSearch.search(nameIdentifierOwner).findAll().size() : -1;
    }

}
