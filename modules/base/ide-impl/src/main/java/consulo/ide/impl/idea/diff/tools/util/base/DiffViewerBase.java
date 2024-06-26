/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diff.tools.util.base;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.dataContext.DataProvider;
import consulo.diff.request.ContentDiffRequest;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.diff.DiffContext;
import consulo.ide.impl.idea.diff.FrameDiffTool;
import consulo.ide.impl.idea.diff.FrameDiffTool.DiffViewer;
import consulo.ide.impl.idea.diff.tools.util.DiffDataKeys;
import consulo.ide.impl.idea.diff.util.DiffTaskQueue;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.ide.impl.idea.openapi.vcs.CalledInBackground;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class DiffViewerBase implements DiffViewer, DataProvider {
  protected static final Logger LOG = Logger.getInstance(DiffViewerBase.class);

  @Nonnull
  private final List<DiffViewerListener> myListeners = new SmartList<>();

  @Nullable
  protected final Project myProject;
  @Nonnull
  protected final DiffContext myContext;
  @Nonnull
  protected final ContentDiffRequest myRequest;

  @Nonnull
  private final DiffTaskQueue myTaskExecutor = new DiffTaskQueue();
  @Nonnull
  private final Alarm myTaskAlarm = new Alarm();
  private volatile boolean myDisposed;

  public DiffViewerBase(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
    myProject = context.getProject();
    myContext = context;
    myRequest = request;
  }

  @Nonnull
  @Override
  public final FrameDiffTool.ToolbarComponents init() {
    processContextHints();
    onInit();

    FrameDiffTool.ToolbarComponents components = new FrameDiffTool.ToolbarComponents();
    components.toolbarActions = createToolbarActions();
    components.popupActions = createPopupActions();
    components.statusPanel = getStatusPanel();

    fireEvent(EventType.INIT);

    rediff(true);
    return components;
  }

  @Override
  @RequiredUIAccess
  public final void dispose() {
    if (myDisposed) return;
    if (!Application.get().isDispatchThread()) LOG.warn(new Throwable("dispose() not from EDT"));

    UIUtil.invokeLaterIfNeeded(() -> {
      if (myDisposed) return;
      myDisposed = true;

      abortRediff();
      updateContextHints();

      fireEvent(EventType.DISPOSE);

      onDispose();
    });
  }

  @RequiredUIAccess
  protected void processContextHints() {
  }

  @RequiredUIAccess
  protected void updateContextHints() {
  }

  @RequiredUIAccess
  public final void scheduleRediff() {
    if (isDisposed()) return;

    abortRediff();
    myTaskAlarm.addRequest(this::rediff, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
  }

  @RequiredUIAccess
  public final void abortRediff() {
    myTaskExecutor.abort();
    myTaskAlarm.cancelAllRequests();
    fireEvent(EventType.REDIFF_ABORTED);
  }

  @RequiredUIAccess
  public final void rediff() {
    rediff(false);
  }

  @RequiredUIAccess
  public void rediff(boolean trySync) {
    if (isDisposed()) return;
    abortRediff();

    fireEvent(EventType.BEFORE_REDIFF);
    onBeforeRediff();

    boolean forceEDT = forceRediffSynchronously();
    int waitMillis = trySync || tryRediffSynchronously() ? ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS : 0;

    myTaskExecutor.executeAndTryWait(
      indicator -> {
        final Runnable callback = performRediff(indicator);
        return () -> {
          callback.run();
          onAfterRediff();
          fireEvent(EventType.AFTER_REDIFF);
        };
      },
      this::onSlowRediff,
      waitMillis,
      forceEDT
    );
  }

  //
  // Getters
  //

  @Nullable
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public ContentDiffRequest getRequest() {
    return myRequest;
  }

  @Nonnull
  public DiffContext getContext() {
    return myContext;
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  //
  // Abstract
  //

  @RequiredUIAccess
  protected boolean tryRediffSynchronously() {
    return myContext.isWindowFocused();
  }

  @RequiredUIAccess
  protected boolean forceRediffSynchronously() {
    // most of performRediff implementations take ReadLock inside. If EDT is holding write lock - this will never happen,
    // and diff will not be calculated. This could happen for diff from FileDocumentManager.
    return Application.get().isWriteAccessAllowed();
  }

  protected List<AnAction> createToolbarActions() {
    List<AnAction> group = new ArrayList<>();
    ContainerUtil.addAll(group, ((ActionGroup)ActionManager.getInstance().getAction(IdeActions.DIFF_VIEWER_TOOLBAR)).getChildren(null));
    return group;
  }

  protected List<AnAction> createPopupActions() {
    List<AnAction> group = new ArrayList<>();
    ContainerUtil.addAll(group, ((ActionGroup)ActionManager.getInstance().getAction(IdeActions.DIFF_VIEWER_POPUP)).getChildren(null));
    return group;
  }

  @Nullable
  protected JComponent getStatusPanel() {
    return null;
  }

  @RequiredUIAccess
  protected void onInit() {
  }

  @RequiredUIAccess
  protected void onSlowRediff() {
  }

  @RequiredUIAccess
  protected void onBeforeRediff() {
  }

  @RequiredUIAccess
  protected void onAfterRediff() {
  }

  @CalledInBackground
  @Nonnull
  protected abstract Runnable performRediff(@Nonnull ProgressIndicator indicator);

  @RequiredUIAccess
  protected void onDispose() {
    Disposer.dispose(myTaskAlarm);
  }

  @Nullable
  protected Navigatable getNavigatable() {
    return null;
  }

  //
  // Listeners
  //

  @RequiredUIAccess
  public void addListener(@Nonnull DiffViewerListener listener) {
    myListeners.add(listener);
  }

  @RequiredUIAccess
  public void removeListener(@Nonnull DiffViewerListener listener) {
    myListeners.remove(listener);
  }

  @Nonnull
  @RequiredUIAccess
  protected List<DiffViewerListener> getListeners() {
    return myListeners;
  }

  @RequiredUIAccess
  private void fireEvent(@Nonnull EventType type) {
    for (DiffViewerListener listener : myListeners) {
      switch (type) {
        case INIT:
          listener.onInit();
          break;
        case DISPOSE:
          listener.onDispose();
          break;
        case BEFORE_REDIFF:
          listener.onBeforeRediff();
          break;
        case AFTER_REDIFF:
          listener.onAfterRediff();
          break;
        case REDIFF_ABORTED:
          listener.onRediffAborted();
          break;
      }
    }
  }

  //
  // Helpers
  //

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (DiffDataKeys.NAVIGATABLE == dataId) {
      return getNavigatable();
    }
    else if (Project.KEY == dataId) {
      return myProject;
    }
    else {
      return null;
    }
  }

  private enum EventType {
    INIT, DISPOSE, BEFORE_REDIFF, AFTER_REDIFF, REDIFF_ABORTED,
  }
}
