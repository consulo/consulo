// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.language.Language;
import consulo.disposer.Disposable;
import consulo.application.event.ApplicationListener;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ReadAction;
import consulo.codeEditor.CaretModel;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListenerImpl;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionType;
import consulo.project.Project;
import consulo.util.lang.function.Condition;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiUtilCore;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.HintListener;
import com.intellij.ui.LightweightHint;
import consulo.util.lang.ThreeState;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.disposer.Disposer;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.FocusEvent;
import java.util.EventObject;
import java.util.concurrent.ExecutorService;

/**
 * @author peter
 */
public abstract class CompletionPhase implements Disposable {
  private static final Logger LOG = Logger.getInstance(CompletionPhase.class);

  public static final CompletionPhase NoCompletion = new CompletionPhase(null) {
    @Override
    public int newCompletionStarted(int time, boolean repeated) {
      return time;
    }
  };

  public final CompletionProgressIndicator indicator;

  protected CompletionPhase(@Nullable CompletionProgressIndicator indicator) {
    this.indicator = indicator;
  }

  @Override
  public void dispose() {
  }

  public abstract int newCompletionStarted(int time, boolean repeated);

  public static class CommittingDocuments extends CompletionPhase {
    private static final ExecutorService ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Completion Preparation", 1);
    boolean replaced;
    private final ActionTracker myTracker;

    CommittingDocuments(@Nullable CompletionProgressIndicator prevIndicator, @Nonnull Editor editor) {
      super(prevIndicator);
      myTracker = new ActionTracker(editor, this);
    }

    public void ignoreCurrentDocumentChange() {
      myTracker.ignoreCurrentDocumentChange();
    }

    private boolean isExpired() {
      return CompletionServiceImpl.getCompletionPhase() != this || myTracker.hasAnythingHappened();
    }

    @Override
    public int newCompletionStarted(int time, boolean repeated) {
      return time;
    }

    @Override
    public void dispose() {
      if (!replaced && indicator != null) {
        indicator.closeAndFinish(true);
      }
    }

    @Override
    public String toString() {
      return "CommittingDocuments{hasIndicator=" + (indicator != null) + '}';
    }

    // @ApiStatus.Internal
    public static void scheduleAsyncCompletion(@Nonnull Editor _editor,
                                               @Nonnull CompletionType completionType,
                                               @Nullable Condition<? super PsiFile> condition,
                                               @Nonnull Project project,
                                               @Nullable CompletionProgressIndicator prevIndicator) {
      Editor topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(_editor);
      int offset = topLevelEditor.getCaretModel().getOffset();

      CommittingDocuments phase = new CommittingDocuments(prevIndicator, topLevelEditor);
      CompletionServiceImpl.setCompletionPhase(phase);
      phase.ignoreCurrentDocumentChange();

      boolean autopopup = prevIndicator == null || prevIndicator.isAutopopupCompletion();

      ReadAction.nonBlocking(() -> {
        // retrieve the injected file from scratch since our typing might have destroyed the old one completely
        PsiFile topLevelFile = PsiDocumentManager.getInstance(project).getPsiFile(topLevelEditor.getDocument());
        Editor completionEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(topLevelEditor, topLevelFile, offset);
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(completionEditor.getDocument());
        if (file == null || autopopup && shouldSkipAutoPopup(completionEditor, file) || condition != null && !condition.value(file)) {
          return null;
        }

        loadContributorsOutsideEdt(completionEditor, file);

        return completionEditor;
      }).withDocumentsCommitted(project).expireWith(phase).expireWhen(() -> phase.isExpired()).finishOnUiThread(IdeaModalityState.current(), completionEditor -> {
        if (completionEditor != null) {
          int time = prevIndicator == null ? 0 : prevIndicator.getInvocationCount();
          CodeCompletionHandlerBase handler = CodeCompletionHandlerBase.createHandler(completionType, false, autopopup, false);
          handler.invokeCompletion(project, completionEditor, time, false);
        }
        else if (phase == CompletionServiceImpl.getCompletionPhase()) {
          CompletionServiceImpl.setCompletionPhase(NoCompletion);
        }
      }).submit(ourExecutor).onError(__ -> AppUIUtil.invokeOnEdt(() -> {
        if (phase == CompletionServiceImpl.getCompletionPhase()) {
          CompletionServiceImpl.setCompletionPhase(NoCompletion);
        }
      }));
    }

    private static void loadContributorsOutsideEdt(Editor editor, PsiFile file) {
      CompletionContributor.forLanguage(PsiUtilCore.getLanguageAtOffset(file, editor.getCaretModel().getOffset()));
    }

    private static boolean shouldSkipAutoPopup(Editor editor, PsiFile psiFile) {
      int offset = editor.getCaretModel().getOffset();
      int psiOffset = Math.max(0, offset - 1);

      PsiElement elementAt = psiFile.findElementAt(psiOffset);
      if (elementAt == null) return true;

      Language language = PsiUtilCore.findLanguageFromElement(elementAt);

      for (CompletionConfidence confidence : CompletionConfidenceEP.forLanguage(language)) {
        final ThreeState result = confidence.shouldSkipAutopopup(elementAt, psiFile, offset);
        if (result != ThreeState.UNSURE) {
          LOG.debug(confidence + " has returned shouldSkipAutopopup=" + result);
          return result == ThreeState.YES;
        }
      }
      return false;
    }

  }

  public static class Synchronous extends CompletionPhase {
    public Synchronous(CompletionProgressIndicator indicator) {
      super(indicator);
    }

    @Override
    public int newCompletionStarted(int time, boolean repeated) {
      CompletionServiceImpl.assertPhase(NoCompletion.getClass()); // will fail and log valuable info
      CompletionServiceImpl.setCompletionPhase(NoCompletion);
      return time;
    }
  }

  public static class BgCalculation extends CompletionPhase {
    boolean modifiersChanged = false;

    public BgCalculation(final CompletionProgressIndicator indicator) {
      super(indicator);
      ApplicationManager.getApplication().addApplicationListener(new ApplicationListener() {
        @Override
        public void beforeWriteActionStart(@Nonnull Object action) {
          if (!indicator.getLookup().isLookupDisposed() && !indicator.isCanceled()) {
            indicator.scheduleRestart();
          }
        }
      }, this);
      if (indicator.isAutopopupCompletion()) {
        // lookup is not visible, we have to check ourselves if editor retains focus
        ((EditorEx)indicator.getEditor()).addFocusListener(new FocusChangeListenerImpl() {
          @Override
          public void focusLost(@Nonnull Editor editor, @Nonnull FocusEvent event) {
            // When ScreenReader is active the lookup gets focus on show and we should not close it.
            if (ScreenReader.isActive() && indicator.getLookup() != null && event.getOppositeComponent() != null && indicator.getLookup().getComponent() != null &&
                // Check the opposite is in the lookup ancestor
                (SwingUtilities.getWindowAncestor(event.getOppositeComponent())) == SwingUtilities.getWindowAncestor(indicator.getLookup().getComponent())) {
              return;
            }
            indicator.closeAndFinish(true);
          }
        }, this);
      }
    }

    @Override
    public int newCompletionStarted(int time, boolean repeated) {
      indicator.closeAndFinish(false);
      return indicator.nextInvocationCount(time, repeated);
    }
  }

  public static class ItemsCalculated extends CompletionPhase {

    public ItemsCalculated(CompletionProgressIndicator indicator) {
      super(indicator);
    }

    @Override
    public int newCompletionStarted(int time, boolean repeated) {
      indicator.closeAndFinish(false);
      return indicator.nextInvocationCount(time, repeated);
    }
  }

  public static abstract class ZombiePhase extends CompletionPhase {

    protected ZombiePhase(@Nullable final LightweightHint hint, final CompletionProgressIndicator indicator) {
      super(indicator);
      @Nonnull Editor editor = indicator.getEditor();
      final HintListener hintListener = new HintListener() {
        @Override
        public void hintHidden(@Nonnull final EventObject event) {
          CompletionServiceImpl.setCompletionPhase(NoCompletion);
        }
      };
      final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void beforeDocumentChange(@Nonnull DocumentEvent e) {
          CompletionServiceImpl.setCompletionPhase(NoCompletion);
        }
      };
      final SelectionListener selectionListener = new SelectionListener() {
        @Override
        public void selectionChanged(@Nonnull SelectionEvent e) {
          CompletionServiceImpl.setCompletionPhase(NoCompletion);
        }
      };
      final CaretListener caretListener = new CaretListener() {
        @Override
        public void caretPositionChanged(@Nonnull CaretEvent e) {
          CompletionServiceImpl.setCompletionPhase(NoCompletion);
        }
      };

      final Document document = editor.getDocument();
      final SelectionModel selectionModel = editor.getSelectionModel();
      final CaretModel caretModel = editor.getCaretModel();


      if (hint != null) {
        hint.addHintListener(hintListener);
      }
      document.addDocumentListener(documentListener, this);
      selectionModel.addSelectionListener(selectionListener, this);
      caretModel.addCaretListener(caretListener, this);

      Disposer.register(this, new Disposable() {
        @Override
        public void dispose() {
          if (hint != null) {
            hint.removeHintListener(hintListener);
          }
        }
      });
    }

  }

  public static class InsertedSingleItem extends ZombiePhase {
    public final Runnable restorePrefix;

    public InsertedSingleItem(CompletionProgressIndicator indicator, Runnable restorePrefix) {
      super(null, indicator);
      this.restorePrefix = restorePrefix;
    }

    @Override
    public int newCompletionStarted(int time, boolean repeated) {
      CompletionServiceImpl.setCompletionPhase(NoCompletion);
      if (repeated) {
        indicator.restorePrefix(restorePrefix);
      }
      return indicator.nextInvocationCount(time, repeated);
    }

  }

  public static class NoSuggestionsHint extends ZombiePhase {
    public NoSuggestionsHint(@Nullable LightweightHint hint, CompletionProgressIndicator indicator) {
      super(hint, indicator);
    }

    @Override
    public int newCompletionStarted(int time, boolean repeated) {
      CompletionServiceImpl.setCompletionPhase(NoCompletion);
      return indicator.nextInvocationCount(time, repeated);
    }

  }

}
