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
package consulo.desktop.awt.internal.diff;

import consulo.application.AllIcons;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.impl.internal.util.DiffTaskQueue;
import consulo.diff.impl.internal.util.SoftHardCacheMap;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.request.*;
import consulo.ide.impl.idea.diff.actions.impl.GoToChangePopupBuilder;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class CacheDiffRequestChainProcessor extends DiffRequestProcessor {
  private static final Logger LOG = Logger.getInstance(CacheDiffRequestChainProcessor.class);

  @Nonnull
  private final DiffRequestChain myRequestChain;

  @Nonnull
  private final SoftHardCacheMap<DiffRequestProducer, DiffRequest> myRequestCache = new SoftHardCacheMap<DiffRequestProducer, DiffRequest>(5, 5);

  @Nonnull
  private final DiffTaskQueue myQueue = new DiffTaskQueue();

  public CacheDiffRequestChainProcessor(@Nullable Project project, @Nonnull DiffRequestChain requestChain) {
    super(project, requestChain);
    myRequestChain = requestChain;
  }

  //
  // Update
  //

  @RequiredUIAccess
  @Override
  protected void reloadRequest() {
    updateRequest(true, false, null);
  }

  @RequiredUIAccess
  public void updateRequest(final boolean force, @Nullable final ScrollToPolicy scrollToChangePolicy) {
    updateRequest(force, true, scrollToChangePolicy);
  }

  @RequiredUIAccess
  public void updateRequest(final boolean force, boolean useCache, @Nullable final ScrollToPolicy scrollToChangePolicy) {
    if (isDisposed()) return;

    List<? extends DiffRequestProducer> requests = myRequestChain.getRequests();
    int index = myRequestChain.getIndex();
    if (index < 0 || index >= requests.size()) {
      applyRequest(NoDiffRequest.INSTANCE, force, scrollToChangePolicy);
      return;
    }

    final DiffRequestProducer producer = requests.get(index);

    DiffRequest request = loadRequestFast(producer, useCache);
    if (request != null) {
      applyRequest(request, force, scrollToChangePolicy);
      return;
    }

    myQueue.executeAndTryWait(indicator -> {
      final DiffRequest request1 = loadRequest(producer, indicator);
      return new Runnable() {
        @RequiredUIAccess
        @Override
        public void run() {
          myRequestCache.put(producer, request1);
          applyRequest(request1, force, scrollToChangePolicy);
        }
      };
    }, new Runnable() {
      @Override
      public void run() {
        applyRequest(new LoadingDiffRequest(producer.getName()), force, scrollToChangePolicy);
      }
    }, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
  }

  @Nullable
  protected DiffRequest loadRequestFast(@Nonnull DiffRequestProducer producer, boolean useCache) {
    if (!useCache) return null;
    return myRequestCache.get(producer);
  }

  @Nonnull
  private DiffRequest loadRequest(@Nonnull DiffRequestProducer producer, @Nonnull ProgressIndicator indicator) {
    try {
      return producer.process(getContext(), indicator);
    }
    catch (ProcessCanceledException e) {
      OperationCanceledDiffRequest request = new OperationCanceledDiffRequest(producer.getName());
      request.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, Collections.<AnAction>singletonList(new ReloadRequestAction(producer)));
      return request;
    }
    catch (DiffRequestProducerException e) {
      return new ErrorDiffRequest(producer, e);
    }
    catch (Exception e) {
      LOG.warn(e);
      return new ErrorDiffRequest(producer, e);
    }
  }

  //
  // Misc
  //

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    super.onDispose();
    myQueue.abort();
    myRequestCache.clear();
  }

  @Nonnull
  @Override
  protected List<AnAction> getNavigationActions() {
    return ContainerUtil.list(new MyPrevDifferenceAction(), new MyNextDifferenceAction(), new MyPrevChangeAction(), new MyNextChangeAction(), createGoToChangeAction());
  }

  //
  // Getters
  //

  @Nonnull
  public DiffRequestChain getRequestChain() {
    return myRequestChain;
  }

  //
  // Navigation
  //

  @RequiredUIAccess
  @Override
  protected boolean hasNextChange() {
    return myRequestChain.getIndex() < myRequestChain.getRequests().size() - 1;
  }

  @RequiredUIAccess
  @Override
  protected boolean hasPrevChange() {
    return myRequestChain.getIndex() > 0;
  }

  @RequiredUIAccess
  @Override
  protected void goToNextChange(boolean fromDifferences) {
    myRequestChain.setIndex(myRequestChain.getIndex() + 1);
    updateRequest(false, fromDifferences ? ScrollToPolicy.FIRST_CHANGE : null);
  }

  @RequiredUIAccess
  @Override
  protected void goToPrevChange(boolean fromDifferences) {
    myRequestChain.setIndex(myRequestChain.getIndex() - 1);
    updateRequest(false, fromDifferences ? ScrollToPolicy.LAST_CHANGE : null);
  }

  @RequiredUIAccess
  @Override
  protected boolean isNavigationEnabled() {
    return myRequestChain.getRequests().size() > 1;
  }

  @Nonnull
  private AnAction createGoToChangeAction() {
    return GoToChangePopupBuilder.create(myRequestChain, index -> {
      if (index >= 0 && index != myRequestChain.getIndex()) {
        myRequestChain.setIndex(index);
        updateRequest();
      }
    });
  }

  //
  // Actions
  //

  protected class ReloadRequestAction extends DumbAwareAction {
    @Nonnull
    private final DiffRequestProducer myProducer;

    public ReloadRequestAction(@Nonnull DiffRequestProducer producer) {
      super("Reload", null, AllIcons.Actions.Refresh);
      myProducer = producer;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      myRequestCache.remove(myProducer);
      updateRequest(true);
    }
  }
}
