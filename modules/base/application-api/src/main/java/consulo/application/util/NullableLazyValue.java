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
package consulo.application.util;

import consulo.annotation.DeprecationInfo;

import org.jspecify.annotations.Nullable;
import java.util.function.Supplier;

/**
 * @author peter
 */
@Deprecated
@DeprecationInfo("Use LazyValue")
public abstract class NullableLazyValue<T> implements Supplier<T>{
  public static <E> NullableLazyValue<E> createValue(final Supplier<? extends E> factory) {
    return new NullableLazyValue<E>() {
      @Override
      protected @Nullable E compute() {
        return factory.get();
      }
    };
  }

  public static <E> NullableLazyValue<E> of(Supplier<E> factory) {
    return createValue(factory);
  }

  private boolean myComputed;
  private @Nullable T myValue;

  protected abstract @Nullable T compute();

  public @Nullable T getValue() {
    if (!myComputed) {
      myValue = compute();
      myComputed = true;
    }
    return myValue;
  }

  @Override
  public @Nullable T get() {
    return getValue();
  }
}
