// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.application.Application;
import consulo.application.NonBlockingReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.UIAccess;
import consulo.util.concurrent.CancellablePromise;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import consulo.language.editor.internal.parameterInfo.ParameterHandlerPopupProxyFactory;

class ParameterInfoTaskRunnerUtil {

  public static final int DEFAULT_PROGRESS_POPUP_DELAY_MS = 1000;

  /**
   * @param progressTitle null means no loading panel should be shown
   */
  static <T> void runTask(Project project, NonBlockingReadAction<T> nonBlockingReadAction, Consumer<T> continuationConsumer, @Nullable String progressTitle, Editor editor) {
    AtomicReference<CancellablePromise<?>> cancellablePromiseRef = new AtomicReference<>();
    Consumer<Boolean> stopAction = startProgressAndCreateStopAction(editor.getProject(), progressTitle, cancellablePromiseRef, editor);

    VisibleAreaListener visibleAreaListener = new CancelProgressOnScrolling(cancellablePromiseRef);

    editor.getScrollingModel().addVisibleAreaListener(visibleAreaListener);

    Component focusOwner = getFocusOwner(project);

    cancellablePromiseRef.set(nonBlockingReadAction.finishOnUiThread(Application::getDefaultModalityState, continuation -> {
      CancellablePromise<?> promise = cancellablePromiseRef.get();
      if (promise != null && promise.isSucceeded() && Objects.equals(focusOwner, getFocusOwner(project))) {
        continuationConsumer.accept(continuation);
      }
    }).expireWith(editor instanceof RealEditor ? ((RealEditor)editor).getDisposable() : project).submit(AppExecutorUtil.getAppExecutorService()).onProcessed(ignore -> {
      stopAction.accept(false);
      editor.getScrollingModel().removeVisibleAreaListener(visibleAreaListener);
    }));
  }

  private static Component getFocusOwner(Project project) {
    return ProjectIdeFocusManager.getInstance(project).getFocusOwner();
  }

  
  private static Consumer<Boolean> startProgressAndCreateStopAction(Project project, String progressTitle, AtomicReference<CancellablePromise<?>> promiseRef, Editor editor) {
    AtomicReference<Consumer<Boolean>> stopActionRef = new AtomicReference<>();

    UIAccess uiAccess = project.getUIAccess();

    Consumer<Boolean> originalStopAction = (cancel) -> {
      stopActionRef.set(null);
      if (cancel) {
        CancellablePromise<?> promise = promiseRef.get();
        if (promise != null) {
          promise.cancel();
        }
      }
    };

    if (progressTitle == null) {
      stopActionRef.set(originalStopAction);
    }
    else {
      AtomicReference<Runnable> hideRef = new AtomicReference<>();
      ScheduledFuture<?> showPopupFuture = uiAccess.getScheduler().schedule(() -> {
        if (!editor.isDisposed() && stopActionRef.get() != null) {
          hideRef.set(project.getApplication().getInstance(ParameterHandlerPopupProxyFactory.class).showLoading(editor, progressTitle, () -> {
            Consumer<Boolean> stopAction = stopActionRef.get();
            if (stopAction != null) {
              stopAction.accept(true);
            }
          }));
        }
      }, project.getApplication().getDefaultModalityState(), DEFAULT_PROGRESS_POPUP_DELAY_MS, TimeUnit.MILLISECONDS);

      stopActionRef.set((cancel) -> {
        try {
          originalStopAction.accept(cancel);
        }
        finally {
          showPopupFuture.cancel(false);
          uiAccess.giveIfNeed(() -> {
            Runnable hide = hideRef.getAndSet(null);
            if (hide != null) {
              hide.run();
            }
          });
        }
      });
    }

    return stopActionRef.get();
  }
}
