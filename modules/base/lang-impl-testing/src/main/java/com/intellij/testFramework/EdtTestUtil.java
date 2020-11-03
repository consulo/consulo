/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.testFramework;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.util.Ref;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ThrowableRunnable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class EdtTestUtil {
  public static void runInEdtAndWait(ThrowableRunnable<Throwable> runnable) {
    try {
      runInEdtAndWaitImpl(runnable);
    }
    catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  public static void runInEdtAndWaitImpl(ThrowableRunnable<Throwable> runnable) throws Throwable {
    Application app = ApplicationManager.getApplication();
    if (app instanceof ApplicationEx) {
      if (app.isDispatchThread()) {
        // reduce stack trace
        runnable.run();
      }
      else {
        Ref<Throwable> exception = Ref.create();
        app.invokeAndWait(() -> {
          try {
            runnable.run();
          }
          catch (Throwable e) {
            exception.set(e);
          }
        });
        ExceptionUtil.rethrowAllAsUnchecked(exception.get());
      }
      return;
    }

    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    }
    else {
      try {
        SwingUtilities.invokeAndWait(() -> {
          try {
            runnable.run();
          }
          catch (Throwable throwable) {
            throw new RuntimeException(throwable);
          }
        });
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }
  }
}
