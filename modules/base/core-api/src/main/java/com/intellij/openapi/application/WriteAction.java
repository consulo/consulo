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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.ObjectUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ThrowableRunnable;
import consulo.annotations.DeprecationInfo;
import consulo.application.AccessRule;
import consulo.application.ApplicationWithOwnWriteThread;

import javax.annotation.Nonnull;

@Deprecated
@DeprecationInfo("Use consulo.application.AccessRule")
public abstract class WriteAction<T> extends BaseActionRunnable<T> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.application.WriteAction");

  @Nonnull
  @Override
  public RunResult<T> execute() {
    return AccessRule.writeAsync(() -> {
      RunResult<T> runResult = new RunResult<>();

      run(runResult);

      return runResult;
    }).getResultSync(-1);
  }

  @Nonnull
  @Deprecated
  public static AccessToken start() {
    // get useful information about the write action
    Class aClass = ObjectUtil.notNull(ReflectionUtil.getGrandCallerClass(), WriteAction.class);
    return start(aClass);
  }

  @Nonnull
  public static AccessToken start(@Nonnull Class clazz) {
    ApplicationWithOwnWriteThread application = (ApplicationWithOwnWriteThread)Application.get();
    return application.acquireWriteActionLockInternal(clazz);
  }

  public static <E extends Throwable> void run(@Nonnull ThrowableRunnable<E> action) throws E {
    AccessRule.writeAsync(action::run).getResultSync(-1);
  }

  public static <T, E extends Throwable> T compute(@Nonnull ThrowableComputable<T, E> action) throws E {
    return AccessRule.writeAsync(action::compute).getResultSync(-1);
  }
}
