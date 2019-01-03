/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.codeInspection.SuppressIntentionActionFromFix;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.featureStatistics.FeatureUsageTrackerImpl;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.PairProcessor;
import com.intellij.util.ThreeState;
import consulo.ui.RequiredUIAccess;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author mike
 */
public class ShowIntentionActionsHandler implements CodeInsightActionHandler {
  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    if (editor instanceof EditorWindow) {
      editor = ((EditorWindow)editor).getDelegate();
      file = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file);
    }

    final LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup != null) {
      lookup.showElementActions();
      return;
    }

    final DaemonCodeAnalyzerImpl codeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    letAutoImportComplete(editor, file, codeAnalyzer);

    ShowIntentionsPass.IntentionsInfo intentions = new ShowIntentionsPass.IntentionsInfo();
    ShowIntentionsPass.getActionsToShow(editor, file, intentions, -1);
    IntentionHintComponent hintComponent = codeAnalyzer.getLastIntentionHint();
    if (hintComponent != null) {
      IntentionHintComponent.PopupUpdateResult result =
              hintComponent.isForEditor(editor) ? hintComponent.updateActions(intentions) : IntentionHintComponent.PopupUpdateResult.HIDE_AND_RECREATE;
      if (result == IntentionHintComponent.PopupUpdateResult.HIDE_AND_RECREATE) {
        hintComponent.hide();
      }
    }

    if (HintManagerImpl.getInstanceImpl().performCurrentQuestionAction()) return;

    //intentions check isWritable before modification: if (!file.isWritable()) return;

    TemplateState state = TemplateManagerImpl.getTemplateState(editor);
    if (state != null && !state.isFinished()) {
      return;
    }

    showIntentionHint(project, editor, file, intentions);
  }

  // added for override into Rider
  @SuppressWarnings("WeakerAccess")
  protected void showIntentionHint(@Nonnull Project project,
                                   @Nonnull Editor editor,
                                   @Nonnull PsiFile file,
                                   @Nonnull ShowIntentionsPass.IntentionsInfo intentions) {
    if (!intentions.isEmpty()) {
      IntentionHintComponent.showIntentionHint(project, file, editor, intentions, true);
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

    int offset = editor.getCaretModel().getOffset();
    PsiElement psiElement = psiFile.findElementAt(offset);
    boolean inProject = psiFile.getManager().isInProject(psiFile);
    try {
      Project project = psiFile.getProject();
      if (action instanceof IntentionActionDelegate) action = ((IntentionActionDelegate)action).getDelegate();
      if (action instanceof IntentionActionDelegate) action = ((IntentionActionDelegate)action).getDelegate();
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
        if (!inProject || psiElement == null || !((PsiElementBaseIntentionAction)action).isAvailable(project, editor, psiElement)) return false;
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
                                                                   @Nonnull PairProcessor<PsiFile, Editor> predicate) {
    Editor editorToApply = null;
    PsiFile fileToApply = null;

    int offset = hostEditor.getCaretModel().getOffset();
    PsiFile injectedFile = InjectedLanguageUtil.findInjectedPsiNoCommit(hostFile, offset);
    if (injectedFile != null) {
      Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(hostEditor, injectedFile);
      if (predicate.process(injectedFile, injectedEditor)) {
        editorToApply = injectedEditor;
        fileToApply = injectedFile;
      }
    }

    if (editorToApply == null && predicate.process(hostFile, hostEditor)) {
      editorToApply = hostEditor;
      fileToApply = hostFile;
    }
    if (editorToApply == null) return null;
    return Pair.create(fileToApply, editorToApply);
  }

  public static boolean chooseActionAndInvoke(@Nonnull PsiFile hostFile,
                                              @Nonnull final Editor hostEditor,
                                              @Nonnull final IntentionAction action,
                                              @Nonnull String text) {
    final Project project = hostFile.getProject();
    return chooseActionAndInvoke(hostFile, hostEditor, action, text, project);
  }

  static boolean chooseActionAndInvoke(@Nonnull PsiFile hostFile,
                                       @Nullable final Editor hostEditor,
                                       @Nonnull final IntentionAction action,
                                       @Nonnull String text,
                                       @Nonnull final Project project) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickFix");
    ((FeatureUsageTrackerImpl)FeatureUsageTracker.getInstance()).getFixesStats().registerInvocation();

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    Pair<PsiFile, Editor> pair = chooseFileForAction(hostFile, hostEditor, action);
    if (pair == null) return false;

    CommandProcessor.getInstance()
            .executeCommand(project, () -> TransactionGuard.getInstance().submitTransactionAndWait(() -> invokeIntention(action, pair.second, pair.first)),
                            text, null);
    return true;
  }

  private static void invokeIntention(@Nonnull IntentionAction action, @Nullable Editor editor, @Nonnull PsiFile file) {
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


  static Pair<PsiFile, Editor> chooseFileForAction(@Nonnull PsiFile hostFile, @Nullable Editor hostEditor, @Nonnull IntentionAction action) {
    return hostEditor == null
           ? Pair.create(hostFile, null)
           : chooseBetweenHostAndInjected(hostFile, hostEditor, (psiFile, editor) -> availableFor(psiFile, editor, action));
  }
}
