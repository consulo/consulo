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
package consulo.ui.desktop.internal;

import com.intellij.ide.IdeEventQueue;
import consulo.components.impl.stores.ComponentStoreImpl;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class AWTUIAccessImpl implements UIAccess {
  public static UIAccess ourInstance = new AWTUIAccessImpl();
  private static final Logger LOGGER = Logger.getInstance(AWTUIAccessImpl.class);

  @Override
  public boolean isValid() {
    return true;
  }

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

  @Nonnull
  @Override
  public <T> AsyncResult<T> give(@Nonnull Supplier<T> supplier) {
    AsyncResult<T> asyncResult = AsyncResult.undefined();
    SwingUtilities.invokeLater(() -> {
      try {
        T result = supplier.get();
        asyncResult.setDone(result);
      }
      catch (Throwable e) {
        LOGGER.error(e);
        asyncResult.rejectWithThrowable(e);
      }
    });
    return asyncResult;
  }

  @Override
  public void giveAndWait(@Nonnull Runnable runnable) {
    ComponentStoreImpl.assertIfInsideSavingSession();
    try {
      SwingUtilities.invokeAndWait(runnable);
    }
    catch (InterruptedException | InvocationTargetException e) {
      //
    }
  }
}