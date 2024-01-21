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
package consulo.application;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.util.function.ThrowableComputable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.function.ThrowableRunnable;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

public final class WriteAction {
  public static void runAndWait(@RequiredUIAccess @RequiredWriteAction Runnable runnable) {
    Application application = Application.get();
    application.runWriteAction(runnable);
  }

  public static <T> T computeAndWait(@RequiredUIAccess @RequiredWriteAction Supplier<T> supplier) {
    Application application = Application.get();
    return application.runWriteAction(supplier::get);
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
