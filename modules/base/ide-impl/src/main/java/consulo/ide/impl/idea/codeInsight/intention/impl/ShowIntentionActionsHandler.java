// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.application.ApplicationManager;
import consulo.application.TransactionGuard;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.externalService.impl.internal.statistic.FeatureUsageTrackerImpl;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.IntentionsUI;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateManagerImpl;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateStateImpl;
import consulo.ide.impl.idea.openapi.application.impl.ApplicationInfoImpl;
import consulo.ide.impl.psi.stubs.StubTextInconsistencyException;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiPredicate;

/**
 * @author mike
 */
public class ShowIntentionActionsHandler implements CodeInsightActionHandler {

  @Override
  public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    invoke(project, editor, file, false);
  }

  public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file, boolean showFeedbackOnEmptyMenu) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    if (editor instanceof EditorWindow) {
      editor = ((EditorWindow)editor).getDelegate();
      file = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file);
    }

    final LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup != null) {
      lookup.showElementActions(null);
      return;
    }

    final DaemonCodeAnalyzerImpl codeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    letAutoImportComplete(editor, file, codeAnalyzer);

    ShowIntentionsPass.IntentionsInfo intentions = ShowIntentionsPass.getActionsToShow(editor, file, true);
    IntentionsUI.getInstance(project).hide();

    if (HintManagerImpl.getInstanceImpl().performCurrentQuestionAction()) return;

    //intentions check isWritable before modification: if (!file.isWritable()) return;

    TemplateStateImpl state = TemplateManagerImpl.getTemplateStateImpl(editor);
    if (state != null && !state.isFinished()) {
      return;
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    Editor finalEditor = editor;
    PsiFile finalFile = file;
    showIntentionHint(project, finalEditor, finalFile, intentions, showFeedbackOnEmptyMenu);
  }

  protected void showIntentionHint(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull ShowIntentionsPass.IntentionsInfo intentions, boolean showFeedbackOnEmptyMenu) {
    if (!intentions.isEmpty()) {
      editor.getScrollingModel().runActionOnScrollingFinished(() -> {
        CachedIntentions cachedIntentions = CachedIntentions.createAndUpdateActions(project, file, editor, intentions);
        IntentionHintComponent.showIntentionHint(project, file, editor, true, cachedIntentions);
      });
    }
    else if (showFeedbackOnEmptyMenu) {
      HintManager.getInstance().showInformationHint(editor, "No context actions available at this location");
    }
  }

  private static void letAutoImportComplete(@Nonnull Editor editor, @Nonnull PsiFile file, DaemonCodeAnalyzerImpl codeAnalyzer) {
    CommandProcessor.getInstance().runUndoTransparentAction(() -> codeAnalyzer.autoImportReferenceAtCursor(editor, file));
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  public static boolean availableFor(@Nonnull PsiFile psiFile, @Nonnull Editor editor, @Nonnull IntentionAction action) {
    if (!psiFile.isValid()) return false;

    try {
      Project project = psiFile.getProject();
      action = IntentionActionDelegate.unwrap(action);
      if (action instanceof SuppressIntentionActionFromFix) {
        final ThreeState shouldBeAppliedToInjectionHost = ((SuppressIntentionActionFromFix)action).isShouldBeAppliedToInjectionHost();
        if (editor instanceof EditorWindow && shouldBeAppliedToInjectionHost == ThreeState.YES) {
          return false;
        }
        if (!(editor instanceof EditorWindow) && shouldBeAppliedToInjectionHost == ThreeState.NO) {
          return false;
        }
      }

      if (action instanceof PsiElementBaseIntentionAction) {
        PsiElementBaseIntentionAction psiAction = (PsiElementBaseIntentionAction)action;
        if (!psiAction.checkFile(psiFile)) {
          return false;
        }
        PsiElement leaf = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (leaf == null || !psiAction.isAvailable(project, editor, leaf)) {
          return false;
        }
      }
      else if (!action.isAvailable(project, editor, psiFile)) {
        return false;
      }
    }
    catch (IndexNotReadyException e) {
      return false;
    }
    return true;
  }

  @Nullable
  public static Pair<PsiFile, Editor> chooseBetweenHostAndInjected(@Nonnull PsiFile hostFile,
                                                                   @Nonnull Editor hostEditor,
                                                                   @Nullable PsiFile injectedFile,
                                                                   @Nonnull BiPredicate<? super PsiFile, ? super Editor> predicate) {
    Editor editorToApply = null;
    PsiFile fileToApply = null;

    Editor injectedEditor = null;
    if (injectedFile != null) {
      injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(hostEditor, injectedFile);
      if (predicate.test(injectedFile, injectedEditor)) {
        editorToApply = injectedEditor;
        fileToApply = injectedFile;
      }
    }

    if (editorToApply == null && hostEditor != injectedEditor && predicate.test(hostFile, hostEditor)) {
      editorToApply = hostEditor;
      fileToApply = hostFile;
    }
    if (editorToApply == null) return null;
    return Pair.create(fileToApply, editorToApply);
  }

  public static boolean chooseActionAndInvoke(@Nonnull PsiFile hostFile, @Nonnull final Editor hostEditor, @Nonnull final IntentionAction action, @Nonnull String text) {
    final Project project = hostFile.getProject();
    return chooseActionAndInvoke(hostFile, hostEditor, action, text, project);
  }

  static boolean chooseActionAndInvoke(@Nonnull PsiFile hostFile, @Nullable final Editor hostEditor, @Nonnull final IntentionAction action, @Nonnull String text, @Nonnull final Project project) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickFix");
    ((FeatureUsageTrackerImpl)FeatureUsageTracker.getInstance()).getFixesStats().registerInvocation();

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    Pair<PsiFile, Editor> pair = chooseFileForAction(hostFile, hostEditor, action);
    if (pair == null) return false;

    CommandProcessor.getInstance().executeCommand(project, () -> TransactionGuard.getInstance().submitTransactionAndWait(() -> invokeIntention(action, pair.second, pair.first)), text, null);

    checkPsiTextConsistency(hostFile);

    return true;
  }

  private static void checkPsiTextConsistency(@Nonnull PsiFile hostFile) {
    if (Registry.is("ide.check.stub.text.consistency") || ApplicationManager.getApplication().isUnitTestMode() && !ApplicationInfoImpl.isInPerformanceTest()) {
      if (hostFile.isValid()) {
        StubTextInconsistencyException.checkStubTextConsistency(hostFile);
      }
    }
  }

  private static void invokeIntention(@Nonnull IntentionAction action, @Nullable Editor editor, @Nonnull PsiFile file) {
    //IntentionsCollector.getInstance().record(file.getProject(), action, file.getLanguage());
    PsiElement elementToMakeWritable = action.getElementToMakeWritable(file);
    if (elementToMakeWritable != null && !FileModificationService.getInstance().preparePsiElementsForWrite(elementToMakeWritable)) {
      return;
    }

    Runnable r = () -> action.invoke(file.getProject(), editor, file);
    if (action.startInWriteAction()) {
      WriteAction.run(r::run);
    }
    else {
      r.run();
    }
  }


  @Nullable
  public static Pair<PsiFile, Editor> chooseFileForAction(@Nonnull PsiFile hostFile, @Nullable Editor hostEditor, @Nonnull IntentionAction action) {
    if (hostEditor == null) {
      return Pair.create(hostFile, null);
    }

    PsiFile injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(hostFile, hostEditor.getCaretModel().getOffset());
    return chooseBetweenHostAndInjected(hostFile, hostEditor, injectedFile, (psiFile, editor) -> availableFor(psiFile, editor, action));
  }
}
