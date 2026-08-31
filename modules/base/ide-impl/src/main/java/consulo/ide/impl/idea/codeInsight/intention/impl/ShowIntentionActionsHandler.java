// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.ide.impl.idea.openapi.application.impl.ApplicationInfoImpl;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.editor.impl.internal.intention.IntentionActionInvoker;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.editor.internal.intention.IntentionsUI;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TemplateState;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.language.impl.internal.psi.stub.StubTextInconsistencyException;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * @author mike
 */
public class ShowIntentionActionsHandler implements CodeInsightActionHandler {
    @Override
    @RequiredUIAccess
    public void invoke(Project project, Editor editor, PsiFile file) {
        invoke(project, editor, file, false);
    }

    @RequiredUIAccess
    public void invoke(Project project, Editor editor, PsiFile file, boolean showFeedbackOnEmptyMenu) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        if (editor instanceof EditorWindow editorWindow) {
            editor = editorWindow.getDelegate();
            file = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file);
        }

        LookupEx lookup = LookupManager.getActiveLookup(editor);
        if (lookup != null) {
            lookup.showElementActions(null);
            return;
        }

        DaemonCodeAnalyzerImpl codeAnalyzer = (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
        letAutoImportComplete(editor, file, codeAnalyzer);

        IntentionsInfo intentions = ShowIntentionsPass.getActionsToShow(editor, file, true);
        IntentionsUI.getInstance(project).hide();

        if (((HintManagerEx) HintManager.getInstance()).performCurrentQuestionAction()) {
            return;
        }

        //intentions check isWritable before modification: if (!file.isWritable()) return;

        TemplateState state = TemplateManager.getInstance(project).getTemplateState(editor);
        if (state != null && !state.isFinished()) {
            return;
        }

        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        Editor finalEditor = editor;
        PsiFile finalFile = file;
        showIntentionHint(project, finalEditor, finalFile, intentions, showFeedbackOnEmptyMenu);
    }

    @RequiredUIAccess
    protected void showIntentionHint(
        Project project,
        Editor editor,
        PsiFile file,
        IntentionsInfo intentions,
        boolean showFeedbackOnEmptyMenu
    ) {
        if (!intentions.isEmpty()) {
            editor.getScrollingModel().runActionOnScrollingFinished(() -> {
                CachedIntentions cachedIntentions = CachedIntentions.createAndUpdateActions(project, file, editor, intentions);
                IntentionsUI.getInstance(project).showPopup(file, editor, cachedIntentions);
            });
        }
        else if (showFeedbackOnEmptyMenu) {
            HintManager.getInstance().showInformationHint(editor, "No context actions available at this location");
        }
    }

    @RequiredUIAccess
    private static void letAutoImportComplete(Editor editor, PsiFile file, DaemonCodeAnalyzerImpl codeAnalyzer) {
        CommandProcessor.getInstance().runUndoTransparentAction(() -> codeAnalyzer.autoImportReferenceAtCursor(editor, file));
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @RequiredReadAction
    public static boolean availableFor(PsiFile psiFile, Editor editor, IntentionAction action) {
        return IntentionActionInvoker.availableFor(psiFile, editor, action);
    }

    public static @Nullable Pair<PsiFile, Editor> chooseBetweenHostAndInjected(
        PsiFile hostFile,
        Editor hostEditor,
        @Nullable PsiFile injectedFile,
        BiPredicate<? super PsiFile, ? super Editor> predicate
    ) {
        return IntentionActionInvoker.chooseBetweenHostAndInjected(hostFile, hostEditor, injectedFile, predicate);
    }

    public static @Nullable Pair<PsiFile, Editor> chooseFileForAction(
        PsiFile hostFile,
        @Nullable Editor hostEditor,
        IntentionAction action
    ) {
        return IntentionActionInvoker.chooseFileForAction(hostFile, hostEditor, action);
    }

    @RequiredUIAccess
    public static boolean chooseActionAndInvoke(
        PsiFile hostFile,
        Editor hostEditor,
        IntentionAction action,
        String text
    ) {
        return IntentionActionInvoker.chooseActionAndInvoke(hostFile, hostEditor, action, text);
    }

    @RequiredUIAccess
    static boolean chooseActionAndInvoke(
        PsiFile hostFile,
        @Nullable Editor hostEditor,
        IntentionAction action,
        String text,
        Project project
    ) {
        return IntentionActionInvoker.chooseActionAndInvoke(hostFile, hostEditor, action, text, project);
    }
}
