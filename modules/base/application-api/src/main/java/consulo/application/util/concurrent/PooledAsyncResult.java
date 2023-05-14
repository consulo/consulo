/*
 * Copyright 2013-2019 consulo.io
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
package consulo.application.util.concurrent;

import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-12
 */
public class PooledAsyncResult {
  @Nonnull
  public static <V> AsyncResult<V> create(@Nonnull Supplier<AsyncResult<V>> callable) {
    AsyncResult<V> result = AsyncResult.undefined();
    AppExecutorUtil.getAppExecutorService().execute(() -> callable.get().notify(result));
    return result;
  }

  @Nonnull
  public static <V> AsyncResult<V> create(@Nonnull Consumer<AsyncResult<V>> consumer) {
    AsyncResult<V> result = AsyncResult.undefined();
    AppExecutorUtil.getAppExecutorService().execute(() -> consumer.accept(result));
    return result;
  }
}