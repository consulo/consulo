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
package consulo.diff.chain;

import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.diff.request.DiffRequest;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;

import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.List;

public class SimpleDiffRequestChain extends UserDataHolderBase implements DiffRequestChain {
  public static SimpleDiffRequestChain fromProducer(DiffRequestProducer producer) {
    return fromProducers(Collections.singletonList(producer));
  }

  public static SimpleDiffRequestChain fromProducers(List<? extends DiffRequestProducer> producers) {
    return fromProducers(producers, -1);
  }

  public static SimpleDiffRequestChain fromProducers(List<? extends DiffRequestProducer> producers, int selectedIndex) {
    SimpleDiffRequestChain chain = new SimpleDiffRequestChain(producers, null);
    if (selectedIndex > 0) chain.setIndex(selectedIndex);
    return chain;
  }

  
  private final List<? extends DiffRequestProducer> myRequests;
  private int myIndex = 0;

  public SimpleDiffRequestChain(DiffRequest request) {
    this(Collections.singletonList(request));
  }

  public SimpleDiffRequestChain(List<? extends DiffRequest> requests) {
    myRequests = ContainerUtil.map(requests, request -> new DiffRequestProducerWrapper(request));
  }

  private SimpleDiffRequestChain(List<? extends DiffRequestProducer> requests, @Nullable Object constructorFlag) {
    assert constructorFlag == null;
    myRequests = requests;
  }

  @Override
  
  public List<? extends DiffRequestProducer> getRequests() {
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
    
    private final DiffRequest myRequest;

    public DiffRequestProducerWrapper(DiffRequest request) {
      myRequest = request;
    }

    
    public DiffRequest getRequest() {
      return myRequest;
    }

    
    @Override
    public String getName() {
      return StringUtil.notNullize(myRequest.getTitle(), "Change");
    }

    
    @Override
    public DiffRequest process(UserDataHolder context, ProgressIndicator indicator) throws DiffRequestProducerException, ProcessCanceledException {
      return myRequest;
    }
  }
}
