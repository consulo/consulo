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
package com.intellij.openapi.application;

import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.ObjectUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ThrowableRunnable;
import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public final class WriteAction {
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use AccessRule.writeAsync()")
  public static AccessToken start() {
    // get useful information about the write action
    Class aClass = ObjectUtil.notNull(ReflectionUtil.getGrandCallerClass(), WriteAction.class);
    return start(aClass);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use AccessRule.writeAsync()")
  public static AccessToken start(@Nonnull Class clazz) {
    return ApplicationManager.getApplication().acquireWriteActionLock(clazz);
  }

  public static void runAndWait(@RequiredUIAccess @RequiredWriteAction Runnable runnable) {
    Application application = Application.get();
    if (application.isDispatchThread()) {
      run(runnable::run);
    }
    else {
      UIAccess uiAccess = application.getLastUIAccess();
      uiAccess.giveAndWaitIfNeed(() -> run(runnable::run));
    }
  }

  public static <T> T computeAndWait(@RequiredUIAccess @RequiredWriteAction Supplier<T> supplier) {
    Application application = Application.get();
    if (application.isDispatchThread()) {
      return compute(supplier::get);
    }
    else {
      UIAccess uiAccess = application.getLastUIAccess();
      return uiAccess.giveAndWaitIfNeed(() -> compute(supplier::get));
    }
  }

  public static <E extends Throwable> void run(@Nonnull ThrowableRunnable<E> action) throws E {
    compute(() -> {
      action.run();
      return null;
    });
  }

  public static <T, E extends Throwable> T compute(@Nonnull ThrowableComputable<T, E> action) throws E {
    Application application = Application.get();
    return application.runWriteAction(action);
  }
}
