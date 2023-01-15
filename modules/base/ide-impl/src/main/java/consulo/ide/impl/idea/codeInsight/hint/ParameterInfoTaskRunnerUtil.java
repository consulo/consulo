// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.ui.ex.awt.LoadingDecorator;
import consulo.ide.impl.idea.ui.components.JBLoadingPanel;
import consulo.application.NonBlockingReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.RealEditor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.AsyncProcessIcon;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.concurrent.EdtScheduledExecutorService;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.concurrent.CancellablePromise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class ParameterInfoTaskRunnerUtil {

  public static final int DEFAULT_PROGRESS_POPUP_DELAY_MS = 1000;

  /**
   * @param progressTitle null means no loading panel should be shown
   */
  static <T> void runTask(Project project, NonBlockingReadAction<T> nonBlockingReadAction, Consumer<T> continuationConsumer, @Nullable String progressTitle, Editor editor) {
    AtomicReference<CancellablePromise<?>> cancellablePromiseRef = new AtomicReference<>();
    Consumer<Boolean> stopAction = startProgressAndCreateStopAction(editor.getProject(), progressTitle, cancellablePromiseRef, editor);

    final VisibleAreaListener visibleAreaListener = new CancelProgressOnScrolling(cancellablePromiseRef);

    editor.getScrollingModel().addVisibleAreaListener(visibleAreaListener);

    final Component focusOwner = getFocusOwner(project);

    cancellablePromiseRef.set(nonBlockingReadAction.finishOnUiThread(IdeaModalityState.defaultModalityState(), continuation -> {
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

  @Nonnull
  private static Consumer<Boolean> startProgressAndCreateStopAction(Project project, String progressTitle, AtomicReference<CancellablePromise<?>> promiseRef, Editor editor) {
    AtomicReference<Consumer<Boolean>> stopActionRef = new AtomicReference<>();

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
      final Disposable disposable = Disposable.newDisposable();
      Disposer.register(project, disposable);

      JBLoadingPanel loadingPanel = new JBLoadingPanel(null, panel -> new LoadingDecorator(panel, disposable, 0, false, new AsyncProcessIcon("ShowParameterInfo")) {
        @Override
        protected NonOpaquePanel customizeLoadingLayer(JPanel parent, JLabel text, AsyncProcessIcon icon) {
          parent.setLayout(new FlowLayout(FlowLayout.LEFT));
          final NonOpaquePanel result = new NonOpaquePanel();
          result.add(icon);
          parent.add(result);
          return result;
        }
      });
      loadingPanel.add(new JBLabel(Image.empty(Image.DEFAULT_ICON_SIZE)));
      loadingPanel.add(new JBLabel(progressTitle));

      ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(loadingPanel, null).setProject(project).setCancelCallback(() -> {
        Consumer<Boolean> stopAction = stopActionRef.get();
        if (stopAction != null) {
          stopAction.accept(true);
        }
        return true;
      });
      JBPopup popup = builder.createPopup();
      Disposer.register(disposable, popup);
      ScheduledFuture<?> showPopupFuture = EdtScheduledExecutorService.getInstance().schedule(() -> {
        if (!popup.isDisposed() && !popup.isVisible() && !editor.isDisposed()) {
          RelativePoint popupPosition = EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
          loadingPanel.startLoading();
          popup.show(popupPosition);
        }
      }, IdeaModalityState.defaultModalityState(), DEFAULT_PROGRESS_POPUP_DELAY_MS, TimeUnit.MILLISECONDS);

      stopActionRef.set((cancel) -> {
        try {
          loadingPanel.stopLoading();
          originalStopAction.accept(cancel);
        }
        finally {
          showPopupFuture.cancel(false);
          UIUtil.invokeLaterIfNeeded(() -> {
            if (popup.isVisible()) {
              popup.setUiVisible(false);
            }
            Disposer.dispose(disposable);
          });
        }
      });
    }

    return stopActionRef.get();
  }
}
