/*
 * Copyright 2013-2025 consulo.io
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
package consulo.application.progress;

import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2025-05-06
 */
public interface ProgressBuilder {
    @Nonnull
    ProgressBuilder cancelable();

    @Nonnull
    ProgressBuilder modal();

    @Nonnull
    @RequiredUIAccess
    <V> CompletableFuture<V> execute(@Nonnull Function<ProgressIndicator, V> function);

    @Nonnull
    @RequiredUIAccess
    CompletableFuture<?> executeVoid(Consumer<ProgressIndicator> consumer);
}
