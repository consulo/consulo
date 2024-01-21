/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.impl.BaseUIAccess;
import consulo.ui.impl.SingleUIAccessScheduler;
import jakarta.annotation.Nonnull;
import org.eclipse.swt.widgets.Display;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtUIAccess extends BaseUIAccess implements UIAccess {
  public static final DesktopSwtUIAccess INSTANCE = new DesktopSwtUIAccess();

  private Display myDisplay;

  public DesktopSwtUIAccess() {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Thread thread = new Thread("SWT Event Queue") {
      @Override
      public void run() {
        myDisplay = new Display();
        countDownLatch.countDown();

        while (true) {
          if (myDisplay.isDisposed()) {
            break;
          }

          boolean readAndDispatch = false;
          try {
            readAndDispatch = myDisplay.readAndDispatch();
          }
          catch (Throwable e) {
            e.printStackTrace();
          }

          if (!readAndDispatch) {
            myDisplay.sleep();
          }
        }
      }
    };
    thread.setDaemon(true);
    thread.setPriority(Thread.MAX_PRIORITY);
    thread.start();

    try {
      countDownLatch.await();
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public Display getDisplay() {
    return myDisplay;
  }

  @Nonnull
  @Override
  public <T> CompletableFuture<T> giveAsync(@Nonnull Supplier<T> supplier) {
    CompletableFuture<T> result = new CompletableFuture<>();
    myDisplay.asyncExec(() -> {
      try {
        result.complete(supplier.get());
      }
      catch (Throwable e) {
        LOG.error(e);
        result.completeExceptionally(e);
      }
    });
    return result;
  }

  @Override
  public void give(@Nonnull Runnable runnable) {
    myDisplay.asyncExec(wrapRunnable(runnable));
  }

  @Override
  public void giveAndWait(@Nonnull Runnable runnable) {
    myDisplay.syncExec(wrapRunnable(runnable));
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
