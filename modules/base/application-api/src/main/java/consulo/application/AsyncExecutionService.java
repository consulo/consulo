// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ui.ModalityState;

import java.util.concurrent.Callable;

/**
 * An internal service not supposed to be used directly
 */
//@ApiStatus.Internal
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class AsyncExecutionService {
  //@NotNull
  //protected abstract ExpirableExecutor createExecutor(@NotNull Executor executor);
  //
  
  protected abstract AppUIExecutor createUIExecutor(ModalityState modalityState);

  
  protected abstract AppUIExecutor createWriteThreadExecutor(ModalityState modalityState);

  
  protected abstract <T> NonBlockingReadAction<T> buildNonBlockingReadAction(Callable<T> computation);

  
  static AsyncExecutionService getService() {
    return Application.get().getInstance(AsyncExecutionService.class);
  }
}
