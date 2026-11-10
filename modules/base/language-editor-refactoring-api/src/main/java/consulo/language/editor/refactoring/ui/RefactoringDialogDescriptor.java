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
package consulo.language.editor.refactoring.ui;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A refactoring dialog with Refactor, Preview and Cancel buttons. Refactor and Preview run {@link #doAction()}, which
 * keeps the dialog open until the refactoring processor reports a successful preparation - see
 * {@link #invokeRefactoring(BaseRefactoringProcessor)} and {@link #closeOKAction()}. Preview is offered only where the
 * usage view is available.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public abstract class RefactoringDialogDescriptor extends DialogDescriptor {
    protected final Project myProject;

    private boolean myPreviewUsages = true;
    private boolean myValid = true;
    private LocalizeValue myErrorText = LocalizeValue.empty();
    private @Nullable Dialog myDialog;
    private @Nullable Toast myErrorToast;
    private boolean myErrorShown;

    protected RefactoringDialogDescriptor(Project project, LocalizeValue title) {
        super(title);
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    public final boolean isPreviewUsages() {
        return myPreviewUsages;
    }

    public void setPreviewResults(boolean previewResults) {
        myPreviewUsages = previewResults;
    }

    @RequiredUIAccess
    public CompletableFuture<DialogValue> showAsync() {
        Dialog dialog = myProject.getApplication().getInstance(DialogService.class).build(myProject, this);
        myDialog = dialog;
        return dialog.showAsync();
    }

    @RequiredUIAccess
    protected abstract Component createContentComponent(Disposable uiDisposable);

    @RequiredUIAccess
    protected abstract void doAction();

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        VerticalLayout root = VerticalLayout.create();
        root.add(createContentComponent(uiDisposable));

        Toast errorToast = Toast.create();
        errorToast.addStyle(ToastStyle.ERROR);
        errorToast.setVisible(false);
        myErrorToast = errorToast;
        root.add(errorToast);

        validateButtons();
        return root;
    }

    @RequiredUIAccess
    protected void doRefactorAction() {
        myPreviewUsages = false;
        doAction();
    }

    @RequiredUIAccess
    private void doPreviewAction() {
        myPreviewUsages = true;
        doAction();
    }

    @RequiredUIAccess
    protected final void closeOKAction() {
        Dialog dialog = myDialog;
        if (dialog != null) {
            dialog.doOkAction(getOkValue());
        }
    }

    protected boolean areButtonsValid() {
        return true;
    }

    protected void canRun() throws ConfigurationException {
        if (!areButtonsValid()) {
            throw new ConfigurationException(LocalizeValue.empty());
        }
    }

    @RequiredUIAccess
    protected void validateButtons() {
        boolean valid = true;
        setErrorText(LocalizeValue.empty());
        try {
            canRun();
        }
        catch (ConfigurationException e) {
            valid = false;
            setErrorText(LocalizeValue.ofNullable(e.getMessage()));
        }
        myValid = valid;
        updateOkButtonState();
    }

    @RequiredUIAccess
    protected void setErrorText(LocalizeValue errorText) {
        myErrorText = errorText;

        Toast errorToast = myErrorToast;
        if (errorToast == null) {
            return;
        }

        boolean shown = errorText.isNotEmpty();
        errorToast.setText(errorText);
        errorToast.setVisible(shown);
        if (shown != myErrorShown) {
            myErrorShown = shown;
            updateSize();
        }
    }

    public LocalizeValue getErrorText() {
        return myErrorText;
    }

    protected boolean hasPreviewButton() {
        return true;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myValid;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(RefactoringLocalize.refactorButton());
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        List<AnAction> actions = new ArrayList<>();
        actions.add(createOkAction());
        if (hasPreviewButton()) {
            actions.add(new PreviewAction());
        }
        actions.add(new DialogCancelAction());
        if (inverseOrder) {
            Collections.reverse(actions);
        }
        return actions.toArray(AnAction.EMPTY_ARRAY);
    }

    @Override
    @RequiredUIAccess
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (action instanceof DialogOkAction) {
            doRefactorAction();
            return false;
        }
        return true;
    }

    @RequiredUIAccess
    protected void invokeRefactoring(BaseRefactoringProcessor processor) {
        processor.setPrepareSuccessfulSwingThreadCallback(this::closeOKAction);
        processor.setPreviewUsages(isPreviewUsages());
        processor.run();
    }

    private final class PreviewAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private PreviewAction() {
            super(RefactoringLocalize.previewButton(), LocalizeValue.empty(), null);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            doPreviewAction();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(doUpdateOkButtonState());
        }
    }
}
