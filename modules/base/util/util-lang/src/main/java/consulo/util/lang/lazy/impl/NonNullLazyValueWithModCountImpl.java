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
package consulo.util.lang.lazy.impl;

import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 24-Apr-22
 */
public class NonNullLazyValueWithModCountImpl<T> implements LazyValue<T> {
  private final Supplier<T> myFactory;
  private final LongSupplier myModCountSupplier;

  private volatile T myValue;
  private volatile long myModificationCount = -1;

  public NonNullLazyValueWithModCountImpl(Supplier<T> factory, LongSupplier modCountSupplier) {
    myFactory = factory;
    myModCountSupplier = modCountSupplier;
  }

  @Nonnull
  @Override
  public T get() {
    T result = myValue;

    long newModificationCount = myModCountSupplier.getAsLong();

    if (result == null || myModificationCount != newModificationCount) {
      result = myFactory.get();
      myValue = result;
      myModificationCount = newModificationCount;
    }
    return result;
  }
}
