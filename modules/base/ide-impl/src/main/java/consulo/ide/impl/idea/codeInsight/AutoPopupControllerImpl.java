// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.PowerSaveMode;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionHandlerBase;
import consulo.ide.impl.idea.codeInsight.completion.CompletionPhase;
import consulo.ide.impl.idea.codeInsight.completion.CompletionProgressIndicator;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.ide.impl.idea.codeInsight.hint.ShowParameterInfoHandler;
import consulo.ide.impl.idea.ide.IdeEventQueue;
import consulo.ide.impl.idea.openapi.editor.EditorActivityManager;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.completion.CompletionType;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public class AutoPopupControllerImpl extends AutoPopupController implements Disposable {
  private final Project myProject;
  private final Alarm myAlarm = new Alarm(this);

  @Inject
  public AutoPopupControllerImpl(Application application, Project project) {
    myProject = project;

    application.getMessageBus().connect(this).subscribe(AnActionListener.class, new AnActionListener() {
      @Override
      public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
        cancelAllRequests();
      }

      @Override
      public void beforeEditorTyping(char c, @Nonnull DataContext dataContext) {
        cancelAllRequests();
      }
    });

    IdeEventQueue.getInstance().addActivityListener(this::cancelAllRequests, this);
  }

  @Override
  public void autoPopupMemberLookup(final Editor editor, @Nullable final Predicate<? super PsiFile> condition) {
    autoPopupMemberLookup(editor, CompletionType.BASIC, condition);
  }

  @Override
  public void autoPopupMemberLookup(final Editor editor, CompletionType completionType, @Nullable final Predicate<? super PsiFile> condition) {
    scheduleAutoPopup(editor, completionType, condition);
  }

  @Override
  public void scheduleAutoPopup(@Nonnull Editor editor, @Nonnull CompletionType completionType, @Nullable final Predicate<? super PsiFile> condition) {
    boolean alwaysAutoPopup = Boolean.TRUE.equals(editor.getUserData(ALWAYS_AUTO_POPUP));
    if (!CodeInsightSettings.getInstance().AUTO_POPUP_COMPLETION_LOOKUP && !alwaysAutoPopup) {
      return;
    }
    if (PowerSaveMode.isEnabled()) {
      return;
    }

    if (!CompletionServiceImpl.isPhase(CompletionPhase.CommittingDocuments.class, CompletionPhase.NoCompletion.getClass())) {
      return;
    }

    final CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    if (currentCompletion != null) {
      currentCompletion.closeAndFinish(true);
    }

    CompletionPhase.CommittingDocuments.scheduleAsyncCompletion(editor, completionType, condition, myProject, null);
  }

  @Override
  public void scheduleAutoPopup(final Editor editor) {
    scheduleAutoPopup(editor, CompletionType.BASIC, null);
  }

  @Override
  public void showParameterInfo(Project project, Editor editor, PsiFile file, int lbraceOffset, PsiElement highlightedElement, boolean requestFocus) {
    ShowParameterInfoHandler.invoke(project, editor, file, lbraceOffset, highlightedElement, requestFocus);
  }

  private void addRequest(final Runnable request, final int delay) {
    Runnable runnable = () -> {
      if (!myAlarm.isDisposed()) myAlarm.addRequest(request, delay);
    };
    ApplicationManager.getApplication().invokeLater(runnable);
  }

  public void cancelAllRequests() {
    myAlarm.cancelAllRequests();
  }

  @Override
  public void autoPopupParameterInfo(@Nonnull final Editor editor, @Nullable final Object highlightedMethod) {
    if (DumbService.isDumb(myProject)) return;
    if (PowerSaveMode.isEnabled()) return;

    ApplicationManager.getApplication().assertIsDispatchThread();
    final CodeInsightSettings settings = CodeInsightSettings.getInstance();
    if (settings.AUTO_POPUP_PARAMETER_INFO) {
      final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
      PsiFile file = documentManager.getPsiFile(editor.getDocument());
      if (file == null) return;

      if (!documentManager.isUncommited(editor.getDocument())) {
        file = documentManager.getPsiFile(InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file).getDocument());
        if (file == null) return;
      }

      Runnable request = () -> {
        if (!myProject.isDisposed() && !DumbService.isDumb(myProject) && !editor.isDisposed() && (EditorActivityManager.getInstance().isVisible(editor))) {
          int lbraceOffset = editor.getCaretModel().getOffset() - 1;
          try {
            PsiFile file1 = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
            if (file1 != null) {
              ShowParameterInfoHandler.invoke(myProject, editor, file1, lbraceOffset, highlightedMethod, false, true, null, e -> {
              });
            }
          }
          catch (IndexNotReadyException ignored) { //anything can happen on alarm
          }
        }
      };

      addRequest(() -> documentManager.performLaterWhenAllCommitted(request), settings.PARAMETER_INFO_DELAY);
    }
  }

  @Override
  public void showCompletionPopup(@Nonnull Editor editor, @Nonnull CompletionType completionType, boolean invokedExplicitly, boolean autopopup, boolean synchronous) {
    new CodeCompletionHandlerBase(completionType, invokedExplicitly, autopopup, synchronous).invokeCompletion(myProject, editor);
  }

  @Override
  public void dispose() {
  }

  @TestOnly
  public void waitForDelayedActions(long timeout, @Nonnull TimeUnit unit) throws TimeoutException {
    long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
    while (System.currentTimeMillis() < deadline) {
      if (myAlarm.isEmpty()) return;
      LockSupport.parkNanos(10_000_000);
      UIUtil.dispatchAllInvocationEvents();
    }
    throw new TimeoutException();
  }
}
