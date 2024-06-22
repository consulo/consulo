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

import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 17/01/2022
 */
public class NullableLazyValueImpl<T> implements LazyValue<T> {
  private final Supplier<T> myFactory;

  protected boolean myComputed;

  @Nullable
  protected volatile T myValue;

  public NullableLazyValueImpl(Supplier<T> factory) {
    myFactory = factory;
  }

  @Nullable
  @Override
  public T getStoredValue() {
    return myValue;
  }

  @Nullable
  @Override
  public T get() {
    if (!myComputed) {
      myValue = myFactory.get();
      myComputed = true;
    }
    return myValue;
  }
}
