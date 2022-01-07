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
package com.intellij.diff.chains;

import com.intellij.diff.requests.DiffRequest;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public class SimpleDiffRequestChain extends UserDataHolderBase implements DiffRequestChain {
  @Nonnull
  private final List<DiffRequestProducerWrapper> myRequests;
  private int myIndex = 0;

  public SimpleDiffRequestChain(@Nonnull DiffRequest request) {
    this(Collections.singletonList(request));
  }

  public SimpleDiffRequestChain(@Nonnull List<? extends DiffRequest> requests) {
    myRequests = ContainerUtil.map(requests, new Function<DiffRequest, DiffRequestProducerWrapper>() {
      @Override
      public DiffRequestProducerWrapper fun(DiffRequest request) {
        return new DiffRequestProducerWrapper(request);
      }
    });
  }

  @Override
  @Nonnull
  public List<DiffRequestProducerWrapper> getRequests() {
    return myRequests;
  }

  @Override
  public int getIndex() {
    return myIndex;
  }

  @Override
  public void setIndex(int index) {
    assert index >= 0 && index < myRequests.size();
    myIndex = index;
  }

  public static class DiffRequestProducerWrapper implements DiffRequestProducer {
    @Nonnull
    private final DiffRequest myRequest;

    public DiffRequestProducerWrapper(@Nonnull DiffRequest request) {
      myRequest = request;
    }

    @Nonnull
    public DiffRequest getRequest() {
      return myRequest;
    }

    @Nonnull
    @Override
    public String getName() {
      return StringUtil.notNullize(myRequest.getTitle(), "Change");
    }

    @Nonnull
    @Override
    public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator)
            throws DiffRequestProducerException, ProcessCanceledException {
      return myRequest;
    }
  }
}
