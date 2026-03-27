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

import org.jspecify.annotations.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2022-01-17
 */
public class NullableLazyValueImpl<T extends @Nullable Object> implements LazyValue<T> {
  private final Supplier<T> myFactory;

  protected boolean myComputed;

  protected volatile @Nullable T myValue = null;

  public NullableLazyValueImpl(Supplier<T> factory) {
    myFactory = factory;
  }

  @Override
  public @Nullable T getStoredValue() {
    return myValue;
  }

  @Override
  @SuppressWarnings("NullAway")
  public T get() {
    if (!myComputed) {
      myValue = myFactory.get();
      myComputed = true;
    }
    // Suppressing NullAway validation here, since annotation @EnsuresNonNullIf("value") on field myComputed doesn't work.
    return myValue;
  }
}
