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

import javax.annotation.Nonnull;

public abstract class WriteAction<T> extends BaseActionRunnable<T> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.application.WriteAction");

  @Nonnull
  @Override
  public RunResult<T> execute() {
    final RunResult<T> result = new RunResult<>(this);

    final Application application = ApplicationManager.getApplication();
    boolean dispatchThread = application.isDispatchThread();
    if (dispatchThread) {
      AccessToken token = start(getClass());
      try {
        result.run();
      }
      finally {
        token.finish();
      }
      return result;
    }

    if (application.isReadAccessAllowed()) {
      LOG.error("Must not start write action from within read action in the other thread - deadlock is coming");
    }

    TransactionGuard.getInstance().submitTransactionAndWait(() -> {
      AccessToken token = start(WriteAction.this.getClass());
      try {
        result.run();
      }
      finally {
        token.finish();
      }
    });

    result.throwException();
    return result;
  }

  @Nonnull
  public static AccessToken start() {
    // get useful information about the write action
    Class aClass = ObjectUtil.notNull(ReflectionUtil.getGrandCallerClass(), WriteAction.class);
    return start(aClass);
  }

  @Nonnull
  public static AccessToken start(@Nonnull Class clazz) {
    return ApplicationManager.getApplication().acquireWriteActionLock(clazz);
  }

  public static <E extends Throwable> void run(@Nonnull ThrowableRunnable<E> action) throws E {
    //Application application = Application.get();
    //if (application instanceof ApplicationWithOwnWriteThread) {
    //  //noinspection RequiredXAction
    //  application.<Void, E>runWriteAction(() -> {
    //    action.run();
    //    return null;
    //  });
    //  return;
    //}

    AccessToken token = start();
    try {
      action.run();
    }
    finally {
      token.finish();
    }
  }

  public static <T, E extends Throwable> T compute(@Nonnull ThrowableComputable<T, E> action) throws E {
    //Application application = Application.get();
    //if (application instanceof ApplicationWithOwnWriteThread) {
    //  //noinspection RequiredXAction
    //  return application.runWriteAction(action);
    //}

    AccessToken token = start();
    try {
      return action.compute();
    }
    finally {
      token.finish();
    }
  }
}
