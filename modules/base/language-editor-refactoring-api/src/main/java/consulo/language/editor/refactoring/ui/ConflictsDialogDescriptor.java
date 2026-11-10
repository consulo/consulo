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

import consulo.application.ReadAction;
import consulo.disposer.Disposable;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Lists the conflicts a refactoring found and asks whether to continue. Where the usage view is available, the
 * conflicts can be shown there instead, from where the refactoring can be retried.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public class ConflictsDialogDescriptor extends DialogDescriptor {
    private final Project myProject;
    private final MultiMap<PsiElement, LocalizeValue> myElementConflictDescription;
    private final Set<LocalizeValue> myConflictDescriptions = new LinkedHashSet<>();
    private final @Nullable Runnable myDoRefactoringRunnable;
    private final boolean myCanContinue;
    private final boolean myCanShowConflictsInView;

    private LocalizeValue myCommandName = LocalizeValue.empty();
    private boolean myShowConflicts;
    private @Nullable Dialog myDialog;

    public ConflictsDialogDescriptor(Project project, MultiMap<PsiElement, LocalizeValue> conflictDescriptions) {
        this(project, conflictDescriptions, null, true, true);
    }

    public ConflictsDialogDescriptor(
        Project project,
        MultiMap<PsiElement, LocalizeValue> conflictDescriptions,
        @Nullable Runnable doRefactoringRunnable
    ) {
        this(project, conflictDescriptions, doRefactoringRunnable, true, true);
    }

    public ConflictsDialogDescriptor(
        Project project,
        MultiMap<PsiElement, LocalizeValue> conflictDescriptions,
        @Nullable Runnable doRefactoringRunnable,
        boolean alwaysShowOkButton,
        boolean canShowConflictsInView
    ) {
        this(
            project,
            RefactoringLocalize.problemsDetectedTitle(),
            conflictDescriptions,
            conflictDescriptions.values(),
            doRefactoringRunnable,
            alwaysShowOkButton || doRefactoringRunnable != null,
            canShowConflictsInView
        );
    }

    /**
     * For conflicts which are not tied to elements; {@link #showInView()} decides what "show in view" shows.
     */
    protected ConflictsDialogDescriptor(Project project, LocalizeValue title, Collection<LocalizeValue> conflictDescriptions) {
        this(project, title, MultiMap.empty(), conflictDescriptions, null, true, true);
    }

    private ConflictsDialogDescriptor(
        Project project,
        LocalizeValue title,
        MultiMap<PsiElement, LocalizeValue> elementConflictDescriptions,
        Collection<LocalizeValue> conflictDescriptions,
        @Nullable Runnable doRefactoringRunnable,
        boolean canContinue,
        boolean canShowConflictsInView
    ) {
        super(title);
        myProject = project;
        myElementConflictDescription = elementConflictDescriptions;
        myConflictDescriptions.addAll(conflictDescriptions);
        myDoRefactoringRunnable = doRefactoringRunnable;
        myCanContinue = canContinue;
        myCanShowConflictsInView = canShowConflictsInView;
    }

    public void setCommandName(LocalizeValue commandName) {
        myCommandName = commandName;
    }

    /**
     * @return whether the user chose to continue. When not, {@link #isShowConflicts()} tells whether the conflicts
     * went to the usage view
     */
    @RequiredUIAccess
    public CompletableFuture<Boolean> showAsync() {
        Dialog dialog = myProject.getApplication().getInstance(DialogService.class).build(myProject, this);
        myDialog = dialog;
        return dialog.showAsync().handle((value, error) -> error == null && value != null);
    }

    public boolean isShowConflicts() {
        return myShowConflicts;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(540, 440);
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        StringBuilder html = new StringBuilder();
        for (LocalizeValue description : myConflictDescriptions) {
            html.append(description.get());
            html.append("<br><br>");
        }

        HtmlView conflicts = HtmlView.create();
        conflicts.render(new HtmlView.RenderData(html.toString()));

        DockLayout layout = DockLayout.create(Space.SMALL);
        layout.top(Label.create(RefactoringLocalize.theFollowingProblemsWereFound()));
        layout.center(conflicts);
        if (myCanContinue) {
            layout.bottom(Label.create(RefactoringLocalize.doYouWishToIgnoreThemAndContinue()));
        }
        return layout;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(RefactoringLocalize.continueButton());
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myCanContinue;
    }

    protected LocalizeValue getShowInViewText() {
        return RefactoringLocalize.showConflictsInViewButton();
    }

    @RequiredUIAccess
    protected void showInView() {
        ReadAction.run(() -> ConflictsUsageView.show(myProject, myElementConflictDescription, myDoRefactoringRunnable, myCommandName));
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        List<AnAction> actions = new ArrayList<>();
        actions.add(createOkAction());
        if (myCanShowConflictsInView) {
            actions.add(new ShowConflictsInViewAction());
        }
        actions.add(new DialogCancelAction());
        if (inverseOrder) {
            Collections.reverse(actions);
        }
        return actions.toArray(AnAction.EMPTY_ARRAY);
    }

    @Override
    public boolean isDefaultAction(AnAction action) {
        if (myCanShowConflictsInView || !myCanContinue) {
            return action instanceof DialogCancelAction;
        }
        return action instanceof DialogOkAction;
    }

    private final class ShowConflictsInViewAction extends DumbAwareAction {
        private ShowConflictsInViewAction() {
            super(getShowInViewText(), LocalizeValue.empty(), null);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            showInView();
            myShowConflicts = true;
            Dialog dialog = myDialog;
            if (dialog != null) {
                dialog.doCancelAction();
            }
        }
    }
}
