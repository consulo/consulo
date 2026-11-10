/*
 * Copyright 2013-2026 consulo.io
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

import consulo.disposer.Disposable;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.RefactoringDialogDescriptor;
import consulo.language.editor.refactoring.util.DeleteUtil;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.StateRestoringCheckBoxWrapper;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Confirms a safe delete and its search options. For a plain delete ({@code delete} set) it also offers to delete
 * without the usage search; {@link #isSafeDelete()} tells which was chosen once the dialog is closed.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public class SafeDeleteDialogDescriptor extends RefactoringDialogDescriptor {
    private final PsiElement[] myElements;
    private final boolean myDelete;
    private final @RequiredUIAccess Consumer<SafeDeleteDialogDescriptor> myCallback;
    private final @Nullable SafeDeleteProcessorDelegate myDelegate;

    private @Nullable CheckBox myCbSafeDelete;
    private @Nullable StateRestoringCheckBoxWrapper myCbSearchInComments;
    private @Nullable StateRestoringCheckBoxWrapper myCbSearchTextOccurrences;

    /**
     * @param callback starts the safe delete, typically through {@link #invokeSafeDelete(SafeDeleteProcessor, Runnable)}
     */
    public SafeDeleteDialogDescriptor(
        Project project,
        PsiElement[] elements,
        boolean delete,
        @RequiredUIAccess Consumer<SafeDeleteDialogDescriptor> callback
    ) {
        super(project, delete ? RefactoringLocalize.deleteTitle() : SafeDeleteHandler.REFACTORING_NAME);
        myElements = elements;
        myDelete = delete;
        myCallback = callback;
        myDelegate = findDelegate(project, elements);
    }

    public boolean isSearchInComments() {
        StateRestoringCheckBoxWrapper checkBox = myCbSearchInComments;
        return checkBox != null && checkBox.getValue();
    }

    public boolean isSearchForTextOccurences() {
        StateRestoringCheckBoxWrapper checkBox = myCbSearchTextOccurrences;
        return checkBox != null && checkBox.getValue();
    }

    public boolean isSafeDelete() {
        CheckBox checkBox = myCbSafeDelete;
        return !myDelete || checkBox == null || checkBox.getValueOrError();
    }

    /**
     * Runs the processor and closes the dialog once it has prepared successfully.
     */
    @RequiredUIAccess
    public void invokeSafeDelete(SafeDeleteProcessor processor, @Nullable Runnable prepareSuccessful) {
        processor.setPrepareSuccessfulSwingThreadCallback(() -> {
            if (prepareSuccessful != null) {
                prepareSuccessful.run();
            }
            closeOKAction();
        });
        processor.setPreviewUsages(isPreviewUsages());
        processor.run();
    }

    @Override
    protected boolean hasPreviewButton() {
        return false;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(CommonLocalize.buttonOk());
    }

    @Override
    @RequiredUIAccess
    protected Component createContentComponent(Disposable uiDisposable) {
        Function<Object, LocalizeValue> messageTemplate = myDelete
            ? RefactoringLocalize::promptDeleteElements
            : RefactoringLocalize::searchForUsagesAndDeleteElements;

        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(DeleteUtil.generateWarningMessage(messageTemplate, myElements)));

        RefactoringSettings settings = RefactoringSettings.getInstance();
        if (myDelete) {
            CheckBox safeDelete = CheckBox.create(RefactoringLocalize.checkboxSafeDeleteWithUsageSearch());
            safeDelete.setId("safeDelete");
            safeDelete.setValue(settings.SAFE_DELETE_WHEN_DELETE);
            safeDelete.addValueListener(e -> updateControls());
            myCbSafeDelete = safeDelete;
            layout.add(safeDelete);
        }

        HorizontalLayout searchOptions = HorizontalLayout.create();
        StateRestoringCheckBoxWrapper searchInComments = new StateRestoringCheckBoxWrapper(RefactoringLocalize.searchInCommentsAndStrings());
        searchInComments.getComponent().setId("searchInComments");
        searchInComments.setValue(
            myDelegate != null ? myDelegate.isToSearchInComments(myElements[0]) : settings.SAFE_DELETE_SEARCH_IN_COMMENTS
        );
        myCbSearchInComments = searchInComments;
        searchOptions.add(searchInComments.getComponent());

        if (needSearchForTextOccurrences()) {
            StateRestoringCheckBoxWrapper searchTextOccurrences =
                new StateRestoringCheckBoxWrapper(RefactoringLocalize.searchForTextOccurrences());
            searchTextOccurrences.getComponent().setId("searchTextOccurrences");
            searchTextOccurrences.setValue(
                myDelegate != null ? myDelegate.isToSearchForTextOccurrences(myElements[0]) : settings.SAFE_DELETE_SEARCH_IN_NON_JAVA
            );
            myCbSearchTextOccurrences = searchTextOccurrences;
            searchOptions.add(searchTextOccurrences.getComponent());
        }
        layout.add(searchOptions);

        updateControls();
        return layout;
    }

    @RequiredUIAccess
    private void updateControls() {
        updateControls(myCbSearchInComments);
        updateControls(myCbSearchTextOccurrences);
    }

    @RequiredUIAccess
    private void updateControls(@Nullable StateRestoringCheckBoxWrapper checkBox) {
        if (checkBox == null) {
            return;
        }
        if (isSafeDelete()) {
            checkBox.makeSelectable();
        }
        else {
            checkBox.makeUnselectable(false);
        }
    }

    @Override
    @RequiredUIAccess
    protected void doAction() {
        if (DumbService.isDumb(myProject)) {
            MessageBoxes.okInfo(RefactoringLocalize.safeDeleteNotAvailableIndexing())
                .title(RefactoringLocalize.refactoringIndexingWarningTitle())
                .showAsync(myProject);
            return;
        }

        saveSettings();

        RefactoringInternalHelper.getInstance().disableWriteChecksDuring(() -> {
            if (isSafeDelete()) {
                myCallback.accept(this);
            }
            else {
                closeOKAction();
            }
        });
    }

    private void saveSettings() {
        RefactoringSettings settings = RefactoringSettings.getInstance();
        CheckBox safeDelete = myCbSafeDelete;
        if (safeDelete != null) {
            settings.SAFE_DELETE_WHEN_DELETE = safeDelete.getValueOrError();
        }
        if (!isSafeDelete()) {
            return;
        }
        if (myDelegate == null) {
            settings.SAFE_DELETE_SEARCH_IN_COMMENTS = isSearchInComments();
            if (myCbSearchTextOccurrences != null) {
                settings.SAFE_DELETE_SEARCH_IN_NON_JAVA = isSearchForTextOccurences();
            }
        }
        else {
            myDelegate.setToSearchInComments(myElements[0], isSearchInComments());
            if (myCbSearchTextOccurrences != null) {
                myDelegate.setToSearchForTextOccurrences(myElements[0], isSearchForTextOccurences());
            }
        }
    }

    private boolean needSearchForTextOccurrences() {
        for (PsiElement element : myElements) {
            if (TextOccurrencesUtil.isSearchTextOccurencesEnabled(element)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable SafeDeleteProcessorDelegate findDelegate(Project project, PsiElement[] elements) {
        if (elements.length != 1) {
            return null;
        }
        return project.getApplication().getExtensionPoint(SafeDeleteProcessorDelegate.class)
            .findFirstSafe(delegate -> delegate.handlesElement(elements[0]));
    }
}
