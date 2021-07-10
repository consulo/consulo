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

import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;
import org.eclipse.swt.widgets.Display;

import javax.annotation.Nonnull;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtUIAccess implements UIAccess {
  public static final DesktopSwtUIAccess INSTANCE = new DesktopSwtUIAccess();

  private Display myDisplay;

  public DesktopSwtUIAccess() {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    new Thread("SWT Event Queue") {
      @Override
      public void run() {
        myDisplay = new Display();
        countDownLatch.countDown();

        while (true) {
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
    }.start();

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

  @Override
  public boolean isValid() {
    return !myDisplay.isDisposed();
  }

  @Nonnull
  @Override
  public <T> AsyncResult<T> give(@Nonnull Supplier<T> supplier) {
    AsyncResult<T> result = AsyncResult.undefined();
    myDisplay.asyncExec(() -> result.setDone(supplier.get()));
    return result;
  }
}
