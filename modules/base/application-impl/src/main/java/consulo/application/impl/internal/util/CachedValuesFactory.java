/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.application.impl.internal.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author Dmitry Avdeev
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CachedValuesFactory {
  <T> CachedValue<T> createCachedValue(@Nonnull CachedValueProvider<T> provider, boolean trackValue);

  <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(@Nonnull ParameterizedCachedValueProvider<T, P> provider, boolean trackValue);

  default void checkProviderForMemoryLeak(@Nonnull CachedValueProvider<?> provider, @Nonnull Key<?> key, @Nonnull UserDataHolder userDataHolder) {
  }

  default boolean areRandomChecksEnabled() {
    return false;
  }

  /**
   * Call this when accessing an already cached value by enabling areRandomChecksEnabled()
   */
  default <T> void applyForRandomCheck(T data, Object provider, Supplier<? extends T> recomputeValue) {
    throw new UnsupportedOperationException();
  }

  default <T> void checkEquivalence(@Nullable T existing, @Nullable T fresh, @Nonnull Class<?> providerClass, @Nullable Supplier<? extends T> recomputeValue) {
  }
}
