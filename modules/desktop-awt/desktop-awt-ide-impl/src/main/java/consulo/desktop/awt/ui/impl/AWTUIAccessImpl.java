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
import consulo.component.store.impl.internal.ComponentStoreImpl;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.BaseUIAccess;
import consulo.ui.impl.SingleUIAccessScheduler;
import jakarta.annotation.Nonnull;

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

  @Override
  public boolean isHeadless() {
    return GraphicsEnvironment.isHeadless();
  }

  @RequiredUIAccess
  @Override
  public int getEventCount() {
    UIAccess.assertIsUIThread();
    return IdeEventQueue.getInstance().getEventCount();
  }

  @RequiredUIAccess
  @Override
  public Runnable markEventCount() {
    int eventCount = getEventCount();
    return () -> IdeEventQueue.getInstance().setEventCount(eventCount);
  }

  @Override
  public void give(@Nonnull Runnable runnable) {
    EventQueue.invokeLater(wrapRunnable(runnable));
  }

  @Nonnull
  @Override
  public <T> CompletableFuture<T> giveAsync(@Nonnull Supplier<T> supplier) {
    CompletableFuture<T> future = new CompletableFuture<>();
    EventQueue.invokeLater(() -> {
      try {
        T result = supplier.get();
        future.complete(result);
      }
      catch (Throwable e) {
        LOG.error(e);
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public void giveAndWait(@Nonnull Runnable runnable) {
    ComponentStoreImpl.assertIfInsideSavingSession();
    try {
      EventQueue.invokeAndWait(wrapRunnable(runnable));
    }
    catch (InterruptedException | InvocationTargetException ignored) {
    }
  }

  @Nonnull
  @Override
  protected SingleUIAccessScheduler createScheduler() {
    Application application = Application.get();
    ApplicationConcurrency concurrency = application.getInstance(ApplicationConcurrency.class);
    return new SingleUIAccessScheduler(this, concurrency.getScheduledExecutorService()) {
      @Override
      public void runWithModalityState(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
        Application.get().invokeLater(runnable, modalityState);
      }
    };
  }
}