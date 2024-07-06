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
package consulo.diff.impl.internal.util;

import consulo.application.progress.ProgressIndicator;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Function;

public class DiffTaskQueue {
  @Nullable
  private ProgressIndicator myProgressIndicator;

  @RequiredUIAccess
  public void abort() {
    if (myProgressIndicator != null) myProgressIndicator.cancel();
    myProgressIndicator = null;
  }

  @RequiredUIAccess
  public void executeAndTryWait(@Nonnull final Function<ProgressIndicator, Runnable> backgroundTask,
                                @Nullable final Runnable onSlowAction,
                                final int waitMillis) {
    executeAndTryWait(backgroundTask, onSlowAction, waitMillis, false);
  }

  @RequiredUIAccess
  public void executeAndTryWait(@Nonnull final Function<ProgressIndicator, Runnable> backgroundTask,
                                @Nullable final Runnable onSlowAction,
                                final int waitMillis,
                                final boolean forceEDT) {
    abort();
    myProgressIndicator = BackgroundTaskUtil.executeAndTryWait(backgroundTask, onSlowAction, waitMillis, forceEDT);
  }
}
