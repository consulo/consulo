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
package consulo.util.lang.lazy;

import consulo.util.lang.lazy.impl.ClearableAtomicLazyValueImpl;
import consulo.util.lang.lazy.impl.ClearableDefaultLazyValueImpl;
import consulo.util.lang.lazy.impl.ClearableNullableLazyValueImpl;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 31/01/2022
 */
public interface ClearableLazyValue<T> extends LazyValue<T> {
  @Nonnull
  static <K> ClearableLazyValue<K> atomicNotNull(@Nonnull Supplier<K> factory) {
    return new ClearableAtomicLazyValueImpl<>(factory);
  }

  @Nonnull
  static <K> ClearableLazyValue<K> nullable(@Nonnull Supplier<K> factory) {
    return new ClearableNullableLazyValueImpl<>(factory);
  }

  @Nonnull
  static <K> ClearableLazyValue<K> notNull(@Nonnull Supplier<K> factory) {
    return new ClearableDefaultLazyValueImpl<>(factory);
  }

  void clear();
}
