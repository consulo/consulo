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

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.ObjectUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.internal.ApplicationWithOwnWriteThread;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-04-24
 */
public final class AccessRule {
  @SuppressWarnings("deprecation")
  public static <E extends Throwable> void read(@RequiredReadAction @Nonnull ThrowableRunnable<E> action) throws E {
    try (AccessToken ignored = Application.get().acquireReadActionLock()) {
      action.run();
    }
  }

  @Nullable
  @SuppressWarnings("deprecation")
  public static <T, E extends Throwable> T read(@RequiredReadAction @Nonnull ThrowableComputable<T, E> action) throws E {
    try (AccessToken ignored = Application.get().acquireReadActionLock()) {
      return action.compute();
    }
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

  @SuppressWarnings("deprecation")
  @Nonnull
  public static <T> AsyncResult<T> writeAsync(@RequiredWriteAction @Nonnull ThrowableComputable<T, Throwable> action) {
    Class aClass = ObjectUtil.notNull(ReflectionUtil.getGrandCallerClass(), WriteAction.class);

    Application application = Application.get();

    if (application instanceof ApplicationWithOwnWriteThread && ((ApplicationWithOwnWriteThread)application).isWriteThreadEnabled()) {
      return ((ApplicationWithOwnWriteThread)application).pushWriteAction(aClass, action);
    }
    else {
      AsyncResult<T> result = AsyncResult.undefined();

      // noinspection RequiredXAction
      try (AccessToken ignored = Application.get().acquireWriteActionLock(aClass)) {
        try {
          result.setDone(action.compute());
        }
        catch (Throwable throwable) {
          result.rejectWithThrowable(throwable);
        }
      }
      return result;
    }
  }
}
