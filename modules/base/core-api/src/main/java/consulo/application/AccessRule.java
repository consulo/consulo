/*
 * Copyright 2013-2018 consulo.io
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
package consulo.application;

import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.logging.Logger;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-04-24
 */
public final class AccessRule {
  private static final Logger LOG = Logger.getInstance(AccessRule.class);

  public static <E extends Throwable> void read(@RequiredReadAction @Nonnull ThrowableRunnable<E> action) throws E {
    Application.get().runReadAction((ThrowableComputable<Object, E>)() -> {
      action.run();
      return null;
    });
  }

  @Nullable
  public static <T, E extends Throwable> T read(@RequiredReadAction @Nonnull ThrowableComputable<T, E> action) throws E {
    return Application.get().runReadAction(action);
  }

  @Nonnull
  public static AsyncResult<Void> readAsync(@RequiredReadAction @Nonnull ThrowableRunnable<Throwable> action) {
    return readAsync(() -> {
      action.run();
      return null;
    });
  }

  @Nonnull
  public static <T> AsyncResult<T> readAsync(@RequiredReadAction @Nonnull ThrowableComputable<T, Throwable> action) {
    AsyncResult<T> result = AsyncResult.undefined();
    Application application = Application.get();
    AppExecutorUtil.getAppExecutorService().execute(() -> {
      try {
        result.setDone(application.runReadAction(action));
      }
      catch (Throwable throwable) {
        LOG.error(throwable);

        result.rejectWithThrowable(throwable);
      }
    });
    return result;
  }

  @Nonnull
  public static AsyncResult<Void> writeAsync(@RequiredWriteAction @Nonnull ThrowableRunnable<Throwable> action) {
    return writeAsync(() -> {
      action.run();
      return null;
    });
  }

  @Nonnull
  public static <T> AsyncResult<T> writeAsync(@RequiredWriteAction @Nonnull ThrowableComputable<T, Throwable> action) {
    AsyncResult<T> result = AsyncResult.undefined();
    AppUIExecutor.onWriteThread().later().execute(() -> {
      try {
        result.setDone(action.compute());
      }
      catch (Throwable throwable) {
        LOG.error(throwable);

        result.rejectWithThrowable(throwable);
      }
    });
    return result;
  }
}
