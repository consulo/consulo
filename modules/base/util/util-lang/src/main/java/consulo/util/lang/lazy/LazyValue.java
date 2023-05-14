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

import consulo.util.lang.lazy.impl.AtomicLazyValueImpl;
import consulo.util.lang.lazy.impl.DefaultLazyValueImpl;
import consulo.util.lang.lazy.impl.NonNullLazyValueWithModCountImpl;
import consulo.util.lang.lazy.impl.NullableLazyValueImpl;

import jakarta.annotation.Nonnull;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 03/01/2022
 */
public interface LazyValue<T> extends Supplier<T> {
  @Nonnull
  static <K> LazyValue<K> atomicNotNull(@Nonnull Supplier<K> factory) {
    return new AtomicLazyValueImpl<>(factory);
  }

  @Nonnull
  static <K> LazyValue<K> notNull(@Nonnull Supplier<K> factory) {
    return new DefaultLazyValueImpl<>(factory);
  }

  @Nonnull
  static <K> LazyValue<K> notNullWithModCount(@Nonnull Supplier<K> factory, @Nonnull LongSupplier modCount) {
    return new NonNullLazyValueWithModCountImpl<>(factory, modCount);
  }

  @Nonnull
  static <K> LazyValue<K> nullable(@Nonnull Supplier<K> factory) {
    return new NullableLazyValueImpl<>(factory);
  }
}
