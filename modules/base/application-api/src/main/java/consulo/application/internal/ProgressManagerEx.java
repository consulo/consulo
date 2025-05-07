/*
 * Copyright 2013-2023 consulo.io
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
package consulo.application.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 03/06/2023
 */
public interface ProgressManagerEx {
    boolean runProcessWithProgressSynchronously(@Nonnull final Task task);

    void runProcessWithProgressInCurrentThread(@Nonnull final Task task,
                                               @Nonnull final ProgressIndicator progressIndicator,
                                               @Nonnull final ModalityState modalityState);

    Future<?> runProcessWithProgressAsynchronously(Task.Backgroundable task,
                                                   ProgressIndicator indicator,
                                                   Runnable continuation,
                                                   ModalityState modalityState);

    ProgressIndicator newBackgroundableProcessIndicator(Task.Backgroundable backgroundable);

    @Nonnull
    @RequiredUIAccess
    <V> CompletableFuture<V> executeTask(@Nullable ComponentManager project,
                                         @Nonnull LocalizeValue titleText,
                                         boolean modal,
                                         boolean cancelable,
                                         Function<ProgressIndicator, V> func);
}
