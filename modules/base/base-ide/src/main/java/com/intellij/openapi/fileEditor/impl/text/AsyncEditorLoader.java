/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.Semaphore;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Extract common implementation from desktop impl
 */
public class AsyncEditorLoader {
  private static final ExecutorService ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("AsyncEditorLoader Pool", 2);
  private static final Key<AsyncEditorLoader> ASYNC_LOADER = Key.create("ASYNC_LOADER");
  private static final int SYNCHRONOUS_LOADING_WAITING_TIME_MS = 200;
  private static final int DOCUMENT_COMMIT_WAITING_TIME_MS = 5_000;
  @Nonnull
  private final Editor myEditor;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final TextEditorImpl myTextEditor;
  @Nonnull
  private final TextEditorComponent myEditorComponent;
  @Nonnull
  private final TextEditorProviderImpl myProvider;
  private final List<Runnable> myDelayedActions = new ArrayList<>();
  private TextEditorState myDelayedState;
  private final AtomicBoolean myLoadingFinished = new AtomicBoolean();

  public AsyncEditorLoader(@Nonnull TextEditorImpl textEditor, @Nonnull TextEditorComponent component, @Nonnull TextEditorProviderImpl provider) {
    myProvider = provider;
    myTextEditor = textEditor;
    myProject = textEditor.myProject;
    myEditorComponent = component;

    myEditor = textEditor.getEditor();
    myEditor.putUserData(ASYNC_LOADER, this);

    myEditorComponent.getComponentContainer().hideContent();
  }

  @RequiredUIAccess
  public void start() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    Future<Runnable> asyncLoading = scheduleLoading();

    boolean showProgress = true;
    if (worthWaiting()) {
      /*
       * Possible alternatives:
       * 1. show "Loading" from the beginning, then it'll be always noticeable at least in fade-out phase
       * 2. show a gray screen for some time and then "Loading" if it's still loading; it'll produce quick background blinking for all editors
       * 3. show non-highlighted and unfolded editor as "Loading" background and allow it to relayout at the end of loading phase
       * 4. freeze EDT a bit and hope that for small editors it'll suffice and for big ones show "Loading" after that.
       * This strategy seems to produce minimal blinking annoyance.
       */
      Runnable continuation = resultInTimeOrNull(asyncLoading);
      if (continuation != null) {
        showProgress = false;
        loadingFinished(continuation);
      }
    }
    if (showProgress) {
      myEditorComponent.getComponentContainer().startLoading();
    }
  }

  private Future<Runnable> scheduleLoading() {
    long commitDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DOCUMENT_COMMIT_WAITING_TIME_MS);

    // we can't return the result of "nonBlocking" call below because it's only finished on EDT later,
    // but we need to get the result of bg calculation in the same EDT event, if it's quick
    CompletableFuture<Runnable> future = new CompletableFuture<>();

    ReadAction.nonBlocking(() -> {
      waitForCommit(commitDeadline);
      Runnable runnable = ProgressManager.getInstance().computePrioritized(() -> {
        try {
          return myTextEditor.loadEditorInBackground();
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception e) {
          Logger.getInstance(AsyncEditorLoader.class).error("Error during async editor loading", e);
          return null;
        }
      });
      future.complete(runnable);
      return runnable;
    }).expireWith(myEditorComponent).expireWith(myProject).finishOnUiThread(ModalityState.any(), result -> loadingFinished(result)).submit(ourExecutor);
    return future;
  }

  private void waitForCommit(long commitDeadlineNs) {
    Document document = myEditor.getDocument();
    PsiDocumentManager pdm = PsiDocumentManager.getInstance(myProject);
    if (!pdm.isCommitted(document) && System.nanoTime() < commitDeadlineNs) {
      Semaphore semaphore = new Semaphore(1);
      pdm.performForCommittedDocument(document, semaphore::up);
      while (System.nanoTime() < commitDeadlineNs && !semaphore.waitFor(10)) {
        ProgressManager.checkCanceled();
      }
    }
  }

  private boolean isDone() {
    return myLoadingFinished.get();
  }

  private boolean worthWaiting() {
    // cannot perform commitAndRunReadAction in parallel to EDT waiting
    return !PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments() && !ApplicationManager.getApplication().isWriteAccessAllowed() && !EditorsSplitters.isOpenedInBulk(myTextEditor.myFile);
  }

  private static <T> T resultInTimeOrNull(@Nonnull Future<T> future) {
    try {
      return future.get(SYNCHRONOUS_LOADING_WAITING_TIME_MS, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException | TimeoutException ignored) {
    }
    catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private void loadingFinished(Runnable continuation) {
    if (!myLoadingFinished.compareAndSet(false, true)) return;
    myEditor.putUserData(ASYNC_LOADER, null);

    if (myEditorComponent.isDisposed()) return;

    if (continuation != null) {
      continuation.run();
    }

    myEditorComponent.loadingFinished();

    if (myDelayedState != null && PsiDocumentManager.getInstance(myProject).isCommitted(myEditor.getDocument())) {
      TextEditorState state = new TextEditorState();
      state.RELATIVE_CARET_POSITION = Integer.MAX_VALUE; // don't do any scrolling
      state.setFoldingState(myDelayedState.getFoldingState());
      myProvider.setStateImpl(myProject, myEditor, state);
      myDelayedState = null;
    }

    for (Runnable runnable : myDelayedActions) {
      myEditor.getScrollingModel().disableAnimation();
      runnable.run();
    }
    myEditor.getScrollingModel().enableAnimation();

    if (FileEditorManager.getInstance(myProject).getSelectedTextEditor() == myEditor) {
      IdeFocusManager.getInstance(myProject).requestFocusInProject(myTextEditor.getPreferredFocusedComponent(), myProject);
    }
    EditorNotifications.getInstance(myProject).updateNotifications(myTextEditor.myFile);
  }

  public static void performWhenLoaded(@Nonnull Editor editor, @Nonnull Runnable runnable) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    AsyncEditorLoader loader = editor.getUserData(ASYNC_LOADER);
    if (loader == null) {
      runnable.run();
    }
    else {
      loader.myDelayedActions.add(runnable);
    }
  }

  @Nonnull
  @RequiredUIAccess
  public TextEditorState getEditorState(@Nonnull FileEditorStateLevel level) {
    ApplicationManager.getApplication().assertIsDispatchThread();


    TextEditorState state = myProvider.getStateImpl(myProject, myEditor, level);
    if (!isDone() && myDelayedState != null) {
      state.setDelayedFoldState(myDelayedState::getFoldingState);
    }
    return state;
  }

  @RequiredUIAccess
  public void setEditorState(@Nonnull final TextEditorState state, boolean exactState) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (!isDone()) {
      myDelayedState = state;
    }

    myProvider.setStateImpl(myProject, myEditor, state);
  }

  public static boolean isEditorLoaded(@Nonnull Editor editor) {
    return editor.getUserData(ASYNC_LOADER) == null;
  }
}
