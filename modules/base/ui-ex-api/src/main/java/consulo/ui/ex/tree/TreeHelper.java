/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.util.lang.ref.Ref;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class TreeHelper {
  public static <T> T calculateYieldingToWriteAction(@RequiredReadAction @Nonnull Supplier<? extends T> producer) throws ProcessCanceledException {
    if (!Registry.is("ide.abstractTreeUi.BuildChildrenInBackgroundYieldingToWriteAction") || ApplicationManager.getApplication().isDispatchThread()) {
      return producer.get();
    }
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null && indicator.isRunning()) {
      return producer.get();
    }

    Ref<T> result = new Ref<>();
    boolean succeeded = ProgressManager.getInstance().runInReadActionWithWriteActionPriority(() -> result.set(producer.get()), indicator);

    if (!succeeded || indicator != null && indicator.isCanceled()) {
      throw new ProcessCanceledException();
    }
    return result.get();
  }
}
