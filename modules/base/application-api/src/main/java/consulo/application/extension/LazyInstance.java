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

package consulo.application.extension;

import consulo.application.util.function.ThrowableComputable;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author peter
 */
@Deprecated
public abstract class LazyInstance<T> implements Supplier<T> {
  @Nonnull
  public static <T> LazyInstance<T> createInstance(@Nonnull final ThrowableComputable<Class<T>, ClassNotFoundException> value) {
    return new LazyInstance<>() {
      @Nonnull
      @Override
      protected Class<T> getInstanceClass() throws ClassNotFoundException {
        return value.compute();
      }
    };
  }

  private volatile T myValue;

  @Nonnull
  protected abstract Class<T> getInstanceClass() throws ClassNotFoundException;

  @Override
  public T get() {
    T value = myValue;
    if (value != null) {
      return value;
    }
    synchronized (this) {
      value = myValue;
      if (value == null) {
        myValue = value = compute();
      }
    }
    return value;
  }

  @Nonnull
  protected final T compute() {
    throw new UnsupportedOperationException("this class dead");
  }

  public T getValue() {
    return get();
  }
}
