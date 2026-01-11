// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.introduce;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.SelectionModel;
import consulo.dataContext.DataContext;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.introduce.inplace.AbstractInplaceIntroducer;
import consulo.language.editor.refactoring.introduce.inplace.OccurrencesChooser;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.usage.UsageInfo;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * Base class for Introduce variable/field/etc refactorings. It provides skeleton for choosing the target and consequent invoking of the
 * given in-place introducer.
 */
public abstract class IntroduceHandler<Target extends IntroduceTarget, Scope extends PsiElement> implements RefactoringActionHandler {

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        if (editor == null || file == null) {
            return;
        }

        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, file)) {
            return;
        }

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        int caretCount = editor.getCaretModel().getCaretCount();
        if (caretCount != 1) {
            cannotPerformRefactoring(project, editor);
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();

        if (selectionModel.hasSelection()) {
            invokeOnSelection(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), project, editor, file);
        }
        else {
            Pair<List<Target>, Integer> targetInfo = collectTargets(file, editor, project);
            List<Target> list = targetInfo.getFirst();
            if (list.isEmpty()) {
                cannotPerformRefactoring(project, editor);
            }
            else if (list.size() == 1) {
                invokeOnTarget(list.get(0), file, editor, project);
            }
            else {
                IntroduceTargetChooser.showIntroduceTargetChooser(
                    editor,
                    list,
                    target -> invokeOnTarget(target, file, editor, project),
                    RefactoringLocalize.introduceTargetChooserExpressionsTitle().get(),
                    targetInfo.getSecond()
                );
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
        //not supported
    }

    @RequiredUIAccess
    private void invokeOnTarget(
        @Nonnull Target target,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    ) {
        LocalizeValue message = checkSelectedTarget(target, file, editor, project);
        if (message.isNotEmpty()) {
            showErrorHint(message, editor, project);
        }
        else {
            invokeScopeStep(target, file, editor, project);
        }
    }

    @RequiredUIAccess
    private void invokeOnSelection(
        int start,
        int end,
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull PsiFile file
    ) {
        Target target = findSelectionTarget(start, end, file, editor, project);
        if (target != null) {
            invokeScopeStep(target, file, editor, project);
        }
        else {
            cannotPerformRefactoring(project, editor);
        }
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private void invokeScopeStep(
        @Nonnull Target target,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    ) {
        List<Scope> scopes = collectTargetScopes(target, editor, file, project);

        if (scopes.isEmpty()) {
            LocalizeValue message = RefactoringLocalize.cannotPerformRefactoringWithReason(getEmptyScopeErrorMessage());
            showErrorHint(message, editor, project);
            return;
        }

        if (scopes.size() == 1) {
            invokeFindUsageStep(target, scopes.get(0), file, editor, project);
        }
        else {
            Scope[] scopeArray = (Scope[]) PsiUtilCore.toPsiElementArray(scopes);
            JBPopup popup = PopupNavigationUtil.getPsiElementPopup(
                scopeArray,
                getScopeRenderer(),
                getChooseScopeTitle(),
                scope -> {
                    invokeFindUsageStep(target, scope, file, editor, project);
                    return false;
                }
            );

            EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, popup);
        }
    }

    @RequiredUIAccess
    private void invokeFindUsageStep(
        @Nonnull Target target,
        @Nonnull Scope scope,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    ) {
        List<UsageInfo> usages = collectUsages(target, scope);
        LocalizeValue message = checkUsages(usages);
        if (message.isNotEmpty()) {
            showErrorHint(message, editor, project);
            return;
        }
        invokeDialogStep(target, scope, usages, file, editor, project);
    }

    private void invokeDialogStep(
        @Nonnull Target target,
        @Nonnull Scope scope,
        @Nonnull List<UsageInfo> usages,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    ) {
        Map<OccurrencesChooser.ReplaceChoice, List<Object>> occurrencesMap = getOccurrenceOptions(target, usages);
        OccurrencesChooser<Object> chooser = new OccurrencesChooser<>(editor) {
            @Override
            @RequiredReadAction
            protected TextRange getOccurrenceRange(Object occurrence) {
                return IntroduceHandler.this.getOccurrenceRange(occurrence);
            }
        };
        chooser.showChooser(occurrencesMap, choice -> startInplaceIntroduce(target, scope, usages, file, editor, project, choice));
    }

    @RequiredUIAccess
    public void startInplaceIntroduce(
        @Nonnull Target target,
        @Nonnull Scope scope,
        @Nonnull List<UsageInfo> usages,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project,
        OccurrencesChooser.ReplaceChoice choice
    ) {
        AbstractInplaceIntroducer<?, ?> introducer = getIntroducer(target, scope, usages, choice, file, editor, project);
        introducer.startInplaceIntroduceTemplate();
    }

    @Nonnull
    @RequiredReadAction
    private TextRange getOccurrenceRange(@Nonnull Object occurrence) {
        if (occurrence instanceof PsiElement element) {
            return element.getTextRange();
        }
        else if (occurrence instanceof UsageInfo usageInfo) {
            Segment segment = usageInfo.getSegment();
            assert segment != null;
            return TextRange.create(segment);
        }
        else {
            //assert occurrence instanceof Target;
            //noinspection unchecked
            return ((Target) occurrence).getTextRange();
        }
    }

    private @Nonnull Map<OccurrencesChooser.ReplaceChoice, List<Object>> getOccurrenceOptions(
        @Nonnull Target target,
        @Nonnull List<UsageInfo> usages
    ) {
        HashMap<OccurrencesChooser.ReplaceChoice, List<Object>> map = new LinkedHashMap<>();

        map.put(OccurrencesChooser.ReplaceChoice.NO, Collections.singletonList(target));
        if (usages.size() > 1) {
            map.put(OccurrencesChooser.ReplaceChoice.ALL, new ArrayList<>(usages));
        }
        return map;
    }


    protected abstract @Nonnull List<UsageInfo> collectUsages(
        @Nonnull Target target,
        @Nonnull Scope scope
    );

    /**
     * @return LocalizeValue.empty() if everything is ok, or a short message describing why it's impossible to perform the refactoring.
     * It will be shown in a balloon popup.
     */
    protected abstract @Nonnull LocalizeValue checkUsages(@Nonnull List<UsageInfo> usages);

    /**
     * @return find all possible scopes for the target to introduce
     */
    protected abstract @Nonnull List<Scope> collectTargetScopes(
        @Nonnull Target target,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull Project project
    );

    /**
     * @return candidates for refactoring (e.g. all expressions which are under caret)
     */
    protected abstract @Nonnull Pair<List<Target>, Integer> collectTargets(
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    );

    /**
     * @param start start offset of the selection
     * @param end   end offset of the selection
     * @return the corresponding target, or null if the range doesn't match any target
     */
    protected abstract @Nullable Target findSelectionTarget(
        int start,
        int end,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    );

    /**
     * @param target to check
     * @return LocalizeValue.empty() if everything is ok, or a short message describing why the refactoring cannot be performed
     */
    protected abstract @Nonnull LocalizeValue checkSelectedTarget(
        @Nonnull Target target,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    );

    protected abstract @Nonnull LocalizeValue getRefactoringName();

    protected abstract @Nullable String getHelpID();

    /**
     * If {@link IntroduceHandler#collectTargetScopes}() returns several possible scopes, the Choose Scope Popup will be shown.
     * It will have this title.
     */
    protected abstract @Nonnull String getChooseScopeTitle();

    /**
     * If {@link IntroduceHandler#collectTargetScopes}() returns several possible scopes, the Choose Scope Popup will be shown.
     * It will use the provided renderer to paint scopes
     */
    protected abstract @Nonnull PsiElementListCellRenderer<Scope> getScopeRenderer();

    /**
     * @return in-place introducer for the refactoring
     */
    protected abstract @Nonnull AbstractInplaceIntroducer<?, ?> getIntroducer(
        @Nonnull Target target,
        @Nonnull Scope scope,
        @Nonnull List<UsageInfo> usages,
        @Nonnull OccurrencesChooser.ReplaceChoice replaceChoice,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Project project
    );

    protected @Nonnull LocalizeValue getEmptyScopeErrorMessage() {
        return RefactoringLocalize.dialogMessageRefactoringNotAvailableInCurrentScope(getRefactoringName());
    }

    @RequiredUIAccess
    protected void showErrorHint(@Nonnull LocalizeValue errorMessage, @Nonnull Editor editor, @Nonnull Project project) {
        CommonRefactoringUtil.showErrorHint(project, editor, errorMessage, getRefactoringName(), getHelpID());
    }

    @RequiredUIAccess
    private void cannotPerformRefactoring(@Nonnull Project project, @Nonnull Editor editor) {
        showErrorHint(RefactoringLocalize.cannotPerformRefactoring(), editor, project);
    }
}
