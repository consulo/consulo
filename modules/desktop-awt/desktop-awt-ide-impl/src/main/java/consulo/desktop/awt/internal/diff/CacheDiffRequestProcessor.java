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
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.impl.internal.util.DiffTaskQueue;
import consulo.diff.impl.internal.util.SoftHardCacheMap;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.request.*;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;

public abstract class CacheDiffRequestProcessor<T> extends DiffRequestProcessor {
  private static final Logger LOG = Logger.getInstance(CacheDiffRequestProcessor.class);

  @Nonnull
  private final SoftHardCacheMap<T, DiffRequest> myRequestCache = new SoftHardCacheMap<>(5, 5);

  @Nonnull
  private final DiffTaskQueue myQueue = new DiffTaskQueue();

  public CacheDiffRequestProcessor(@Nullable Project project) {
    super(project);
  }

  public CacheDiffRequestProcessor(@Nullable Project project, @Nonnull String place) {
    super(project, place);
  }

  public CacheDiffRequestProcessor(@jakarta.annotation.Nullable Project project, @Nonnull UserDataHolder context) {
    super(project, context);
  }

  //
  // Abstract
  //

  @jakarta.annotation.Nullable
  protected abstract String getRequestName(@Nonnull T provider);

  protected abstract T getCurrentRequestProvider();

  @Nonnull
  protected abstract DiffRequest loadRequest(@Nonnull T provider, @Nonnull ProgressIndicator indicator)
          throws ProcessCanceledException, DiffRequestProducerException;

  //
  // Update
  //

  @Override
  protected void reloadRequest() {
    updateRequest(true, false, null);
  }

  @Override
  @RequiredUIAccess
  public void updateRequest(final boolean force, @Nullable final ScrollToPolicy scrollToChangePolicy) {
    updateRequest(force, true, scrollToChangePolicy);
  }

  @RequiredUIAccess
  public void updateRequest(final boolean force, boolean useCache, @Nullable final ScrollToPolicy scrollToChangePolicy) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (isDisposed()) return;

    final T requestProvider = getCurrentRequestProvider();
    if (requestProvider == null) {
      applyRequest(NoDiffRequest.INSTANCE, force, scrollToChangePolicy);
      return;
    }

    DiffRequest cachedRequest = useCache ? loadRequestFast(requestProvider) : null;
    if (cachedRequest != null) {
      applyRequest(cachedRequest, force, scrollToChangePolicy);
      return;
    }

    myQueue.executeAndTryWait(indicator -> {
      final DiffRequest request = doLoadRequest(requestProvider, indicator);
      return () -> {
        myRequestCache.put(requestProvider, request);
        applyRequest(request, force, scrollToChangePolicy);
      };
    }, () -> {
      applyRequest(new LoadingDiffRequest(getRequestName(requestProvider)), force, scrollToChangePolicy);
    }, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
  }

  @jakarta.annotation.Nullable
  protected DiffRequest loadRequestFast(@Nonnull T provider) {
    return myRequestCache.get(provider);
  }

  @Nonnull
  private DiffRequest doLoadRequest(@Nonnull T provider, @Nonnull ProgressIndicator indicator) {
    String name = getRequestName(provider);
    try {
      return loadRequest(provider, indicator);
    }
    catch (ProcessCanceledException e) {
      OperationCanceledDiffRequest request = new OperationCanceledDiffRequest(name);
      request.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, Collections.<AnAction>singletonList(new ReloadRequestAction(provider)));
      return request;
    }
    catch (DiffRequestProducerException e) {
      return new ErrorDiffRequest(name, e);
    }
    catch (Exception e) {
      LOG.warn(e);
      return new ErrorDiffRequest(name, e);
    }
  }

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    super.onDispose();
    myQueue.abort();
    myRequestCache.clear();
  }

  //
  // Actions
  //

  protected class ReloadRequestAction extends DumbAwareAction {
    @Nonnull
    private final T myProducer;

    public ReloadRequestAction(@Nonnull T provider) {
      super("Reload", null, AllIcons.Actions.Refresh);
      myProducer = provider;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      myRequestCache.remove(myProducer);
      updateRequest(true);
    }
  }
}
