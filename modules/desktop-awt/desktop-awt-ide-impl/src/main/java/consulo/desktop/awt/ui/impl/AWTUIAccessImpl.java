/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.ProcessCanceledException;
import consulo.component.store.impl.internal.ComponentStoreImpl;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.impl.BaseUIAccess;
import consulo.ui.impl.SingleUIAccessScheduler;
import consulo.util.concurrent.AsyncResult;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class AWTUIAccessImpl extends BaseUIAccess implements UIAccess {
  public static UIAccess ourInstance = new AWTUIAccessImpl();
  private static final Logger LOGGER = Logger.getInstance(AWTUIAccessImpl.class);

  @Override
  public void execute(Runnable command) {
    SwingUtilities.invokeLater(command);
  }

  @Override
  public boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  @Override
  public boolean isInModalContext() {
    return false;
  }

  
  @Override
  public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
    CompletableFuture<T> future = new CompletableFuture<>();
    SwingUtilities.invokeLater(() -> {
      try {
        T result = supplier.get();
        future.complete(result);
      }
      catch (ProcessCanceledException exception) {
          future.cancel(false);
      }
      catch (Throwable e) {
        LOGGER.error(e);
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  
  @Override
  public <T> AsyncResult<T> give(Supplier<T> supplier) {
    AsyncResult<T> asyncResult = AsyncResult.undefined();
    SwingUtilities.invokeLater(() -> {
      try {
        T result = supplier.get();
        asyncResult.setDone(result);
      }
      catch (ProcessCanceledException exception) {
          asyncResult.rejectWithThrowable(exception);
      }
      catch (Throwable e) {
        LOGGER.error(e);
        asyncResult.rejectWithThrowable(e);
      }
    });
    return asyncResult;
  }

  @Override
  public void giveAndWait(Runnable runnable) {
    ComponentStoreImpl.assertIfInsideSavingSession();
    try {
      SwingUtilities.invokeAndWait(runnable);
    }
    catch (InterruptedException | InvocationTargetException e) {
      //
    }
  }

  
  @Override
  protected SingleUIAccessScheduler createScheduler() {
    Application application = Application.get();
    ApplicationConcurrency concurrency = application.getInstance(ApplicationConcurrency.class);
    return new SingleUIAccessScheduler(this, concurrency.getScheduledExecutorService()) {
      @Override
      public void runWithModalityState(Runnable runnable, ModalityState modalityState) {
        Application.get().invokeLater(runnable, modalityState);
      }
    };
  }
}