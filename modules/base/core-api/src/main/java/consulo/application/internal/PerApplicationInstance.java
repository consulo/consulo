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
package consulo.application.internal;

import com.intellij.openapi.application.Application;
import consulo.disposer.Disposer;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-06-27
 */
public final class PerApplicationInstance<V> implements Provider<V>, Supplier<V> {
  @Nonnull
  public static <T> PerApplicationInstance<T> of(@Nonnull Class<T> clazz) {
    return new PerApplicationInstance<>(clazz);
  }

  private volatile V myValue;

  private final Class<V> myTargetClass;

  private PerApplicationInstance(Class<V> targetClass) {
    myTargetClass = targetClass;
  }

  @Override
  @Nonnull
  public V get() {
    V oldValue = myValue;
    if (oldValue == null) {
      synchronized (this) {
        oldValue = myValue;
        if (oldValue == null) {
          Application application = Application.get();
          V newValue = application.getInjectingContainer().getInstance(myTargetClass);
          Disposer.register(application, () -> myValue = null);
          myValue = newValue;
          return newValue;
        }
      }
    }

    return oldValue;
  }
}
