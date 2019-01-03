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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.annotations.DeprecationInfo;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Deprecated
@DeprecationInfo("Desktop only")
@SuppressWarnings("deprecation")
public class DesktopAsyncEditorLoader {
  private static final ExecutorService ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("AsyncEditorLoader pool", 2);
  private static final Key<DesktopAsyncEditorLoader> ASYNC_LOADER = Key.create("ASYNC_LOADER");
  private static final int SYNCHRONOUS_LOADING_WAITING_TIME_MS = 200;
  private static final int RETRY_TIME_MS = 10;
  @Nonnull
  private final Editor myEditor;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final DesktopTextEditorImpl myTextEditor;
  @Nonnull
  private final TextEditorComponent myEditorComponent;
  @Nonnull
  private final DesktopTextEditorProvider myProvider;
  private final List<Runnable> myDelayedActions = new ArrayList<>();
  private TextEditorState myDelayedState;
  private final CompletableFuture<?> myLoadingFinished = new CompletableFuture<>();

  DesktopAsyncEditorLoader(@Nonnull DesktopTextEditorImpl textEditor, @Nonnull TextEditorComponent component, @Nonnull DesktopTextEditorProvider provider) {
    myProvider = provider;
    myTextEditor = textEditor;
    myProject = textEditor.myProject;
    myEditorComponent = component;

    myEditor = textEditor.getEditor();
    myEditor.putUserData(ASYNC_LOADER, this);

    myEditorComponent.getContentPanel().setVisible(false);
  }

  Future<?> startNew() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(myProject);

    UIAccess uiAccess = UIAccess.current();
    long startStamp = myEditor.getDocument().getModificationStamp();

    myEditorComponent.startLoading();

    psiDocumentManager.commitAllDocumentsAsync().doWhenDone(() -> {
      uiAccess.give(myTextEditor::loadEditorInBackground).doWhenDone((continuation) -> {
        if (startStamp == myEditor.getDocument().getModificationStamp()) {
          uiAccess.give(() -> loadingFinished(continuation));
        }
        else if (!myProject.isDisposed() && !myEditorComponent.isDisposed()) {
          startNew();
        }
      });
    });
    return CompletableFuture.completedFuture(null);
  }

  private static void invokeLater(Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
  }

  private boolean worthWaiting() {
    // cannot perform commitAndRunReadAction in parallel to EDT waiting
    return !PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments() && !ApplicationManager.getApplication().isWriteAccessAllowed();
  }

  private static <T> T resultInTimeOrNull(Future<T> future, long timeMs) {
    try {
      return future.get(timeMs, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException | TimeoutException ignored) {
    }
    catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private void loadingFinished(Runnable continuation) {
    if (myLoadingFinished.isDone()) return;
    myLoadingFinished.complete(null);
    myEditor.putUserData(ASYNC_LOADER, null);

    if (myEditorComponent.isDisposed()) return;

    if (continuation != null) {
      continuation.run();
    }

    if (myEditorComponent.isLoading()) {
      myEditorComponent.stopLoading();
    }
    myEditorComponent.getContentPanel().setVisible(true);

    if (myDelayedState != null) {
      TextEditorState state = new TextEditorState();
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
    DesktopAsyncEditorLoader loader = editor.getUserData(ASYNC_LOADER);
    if (loader == null) {
      runnable.run();
    }
    else {
      loader.myDelayedActions.add(runnable);
    }
  }

  @Nonnull
  TextEditorState getEditorState(@Nonnull FileEditorStateLevel level) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    TextEditorState state = myProvider.getStateImpl(myProject, myEditor, level);
    if (!myLoadingFinished.isDone() && myDelayedState != null) {
      state.setDelayedFoldState(myDelayedState::getFoldingState);
    }
    return state;
  }

  void setEditorState(@Nonnull final TextEditorState state) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (!myLoadingFinished.isDone()) {
      myDelayedState = state;
    }

    myProvider.setStateImpl(myProject, myEditor, state);
  }

  public static boolean isEditorLoaded(@Nonnull Editor editor) {
    return editor.getUserData(ASYNC_LOADER) == null;
  }
}