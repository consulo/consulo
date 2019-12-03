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
package com.intellij.diff.impl;

import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.requests.*;
import com.intellij.diff.tools.util.SoftHardCacheMap;
import com.intellij.diff.util.DiffTaskQueue;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.DiffUserDataKeysEx.ScrollToPolicy;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

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

  public CacheDiffRequestProcessor(@javax.annotation.Nullable Project project, @Nonnull UserDataHolder context) {
    super(project, context);
  }

  //
  // Abstract
  //

  @javax.annotation.Nullable
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

  @javax.annotation.Nullable
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
