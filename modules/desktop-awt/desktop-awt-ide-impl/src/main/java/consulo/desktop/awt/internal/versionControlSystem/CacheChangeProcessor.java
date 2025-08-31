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
package consulo.desktop.awt.internal.versionControlSystem;

import consulo.application.AllIcons;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.desktop.awt.internal.diff.DiffRequestProcessor;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.impl.internal.util.DiffTaskQueue;
import consulo.diff.impl.internal.util.SoftHardCacheMap;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.request.*;
import consulo.application.impl.internal.progress.ProgressWindow;
import consulo.versionControlSystem.internal.CacheChangeProcessorBridge;
import consulo.ide.impl.idea.openapi.vcs.CalledInAwt;
import consulo.ide.impl.idea.openapi.vcs.CalledInBackground;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.diff.ChangeDiffRequestProducer;
import consulo.versionControlSystem.impl.internal.change.FakeRevision;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Collections;
import java.util.List;

public abstract class CacheChangeProcessor extends DiffRequestProcessor implements CacheChangeProcessorBridge {
  private static final Logger LOG = Logger.getInstance(CacheChangeProcessor.class);

  @Nonnull
  private final SoftHardCacheMap<Change, Pair<Change, DiffRequest>> myRequestCache = new SoftHardCacheMap<Change, Pair<Change, DiffRequest>>(5, 5);

  @Nullable
  private Change myCurrentChange;

  @Nonnull
  private final DiffTaskQueue myQueue = new DiffTaskQueue();

  public CacheChangeProcessor(@Nonnull Project project) {
    super(project);
  }

  public CacheChangeProcessor(@Nonnull Project project, @Nonnull String place) {
    super(project, place);
  }

  //
  // Abstract
  //

  @Nonnull
  protected abstract List<Change> getSelectedChanges();

  @Nonnull
  protected abstract List<Change> getAllChanges();

  protected abstract void selectChange(@Nonnull Change change);

  //
  // Update
  //

  @Override
  protected void reloadRequest() {
    updateRequest(true, false, null);
  }

  @CalledInAwt
  public void updateRequest(boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
    updateRequest(force, true, scrollToChangePolicy);
  }

  @CalledInAwt
  public void updateRequest(final boolean force, boolean useCache, @jakarta.annotation.Nullable final ScrollToPolicy scrollToChangePolicy) {
    if (isDisposed()) return;
    final Change change = myCurrentChange;

    DiffRequest cachedRequest = loadRequestFast(change, useCache);
    if (cachedRequest != null) {
      applyRequest(cachedRequest, force, scrollToChangePolicy);
      return;
    }

    // TODO: check if current loading change is the same as we want to load now? (and not interrupt loading)
    myQueue.executeAndTryWait(indicator -> {
      final DiffRequest request = loadRequest(change, indicator);
      return new Runnable() {
        @Override
        @CalledInAwt
        public void run() {
          myRequestCache.put(change, Pair.create(change, request));
          applyRequest(request, force, scrollToChangePolicy);
        }
      };
    }, new Runnable() {
      @Override
      public void run() {
        applyRequest(new LoadingDiffRequest(ChangeDiffRequestProducer.getRequestTitle(change)), force, scrollToChangePolicy);
      }
    }, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
  }

  @Nullable
  @CalledInAwt
  @Contract("null, _ -> !null")
  protected DiffRequest loadRequestFast(@Nullable Change change, boolean useCache) {
    if (change == null) return NoDiffRequest.INSTANCE;

    if (useCache) {
      Pair<Change, DiffRequest> pair = myRequestCache.get(change);
      if (pair != null) {
        Change oldChange = pair.first;
        if (ChangeDiffRequestProducer.isEquals(oldChange, change)) {
          return pair.second;
        }
      }
    }

    if (change.getBeforeRevision() instanceof FakeRevision || change.getAfterRevision() instanceof FakeRevision) {
      return new LoadingDiffRequest(ChangeDiffRequestProducer.getRequestTitle(change));
    }

    return null;
  }

  @Nonnull
  @CalledInBackground
  private DiffRequest loadRequest(@Nonnull Change change, @Nonnull ProgressIndicator indicator) {
    ChangeDiffRequestProducer presentable = ChangeDiffRequestProducer.create(getProject(), change);
    if (presentable == null) return new ErrorDiffRequest("Can't show diff");
    try {
      return presentable.process(getContext(), indicator);
    }
    catch (ProcessCanceledException e) {
      OperationCanceledDiffRequest request = new OperationCanceledDiffRequest(presentable.getName());
      request.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, Collections.<AnAction>singletonList(new ReloadRequestAction(change)));
      return request;
    }
    catch (DiffRequestProducerException e) {
      return new ErrorDiffRequest(presentable, e);
    }
    catch (Exception e) {
      LOG.warn(e);
      return new ErrorDiffRequest(presentable, e);
    }
  }

  //
  // Impl
  //

  @Override
  @CalledInAwt
  protected void onDispose() {
    super.onDispose();
    myQueue.abort();
    myRequestCache.clear();
  }

  @Nonnull
  @Override
  public Project getProject() {
    return super.getProject();
  }

  //
  // Navigation
  //

  /*
   * Multiple selection:
   * - iterate inside selection
   *
   * Single selection:
   * - iterate all changes
   * - update selection after movement
   *
   * current element should always be among allChanges and selection (if they are not empty)
   */

  public void clear() {
    myCurrentChange = null;
    updateRequest();
  }

  @CalledInAwt
  public void refresh() {
    List<Change> selectedChanges = getSelectedChanges();

    if (selectedChanges.isEmpty()) {
      myCurrentChange = null;
      updateRequest();
      return;
    }

    Change selectedChange = myCurrentChange != null ? ContainerUtil.find(selectedChanges, myCurrentChange) : null;
    if (selectedChange == null) {
      myCurrentChange = selectedChanges.get(0);
      updateRequest();
      return;
    }

    if (!ChangeDiffRequestProducer.isEquals(myCurrentChange, selectedChange)) {
      myCurrentChange = selectedChange;
      updateRequest();
    }
  }

  @Override
  protected boolean hasNextChange() {
    if (myCurrentChange == null) return false;

    List<Change> selectedChanges = getSelectedChanges();
    if (selectedChanges.isEmpty()) return false;

    if (selectedChanges.size() > 1) {
      int index = selectedChanges.indexOf(myCurrentChange);
      return index != -1 && index < selectedChanges.size() - 1;
    }
    else {
      List<Change> allChanges = getAllChanges();
      int index = allChanges.indexOf(myCurrentChange);
      return index != -1 && index < allChanges.size() - 1;
    }
  }

  @Override
  protected boolean hasPrevChange() {
    if (myCurrentChange == null) return false;

    List<Change> selectedChanges = getSelectedChanges();
    if (selectedChanges.isEmpty()) return false;

    if (selectedChanges.size() > 1) {
      int index = selectedChanges.indexOf(myCurrentChange);
      return index != -1 && index > 0;
    }
    else {
      List<Change> allChanges = getAllChanges();
      int index = allChanges.indexOf(myCurrentChange);
      return index != -1 && index > 0;
    }
  }

  @Override
  protected void goToNextChange(boolean fromDifferences) {
    List<Change> selectedChanges = getSelectedChanges();
    List<Change> allChanges = getAllChanges();

    if (selectedChanges.size() > 1) {
      int index = selectedChanges.indexOf(myCurrentChange);
      myCurrentChange = selectedChanges.get(index + 1);
    }
    else {
      int index = allChanges.indexOf(myCurrentChange);
      myCurrentChange = allChanges.get(index + 1);
      selectChange(myCurrentChange);
    }

    updateRequest(false, fromDifferences ? ScrollToPolicy.FIRST_CHANGE : null);
  }

  @Override
  protected void goToPrevChange(boolean fromDifferences) {
    List<Change> selectedChanges = getSelectedChanges();
    List<Change> allChanges = getAllChanges();

    if (selectedChanges.size() > 1) {
      int index = selectedChanges.indexOf(myCurrentChange);
      myCurrentChange = selectedChanges.get(index - 1);
    }
    else {
      int index = allChanges.indexOf(myCurrentChange);
      myCurrentChange = allChanges.get(index - 1);
      selectChange(myCurrentChange);
    }

    updateRequest(false, fromDifferences ? ScrollToPolicy.LAST_CHANGE : null);
  }

  @Override
  protected boolean isNavigationEnabled() {
    return getSelectedChanges().size() > 1 || getAllChanges().size() > 1;
  }

  //
  // Actions
  //

  protected class ReloadRequestAction extends DumbAwareAction {
    @Nonnull
    private final Change myChange;

    public ReloadRequestAction(@Nonnull Change change) {
      super("Reload", null, AllIcons.Actions.Refresh);
      myChange = change;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      myRequestCache.remove(myChange);
      updateRequest(true);
    }
  }
}
