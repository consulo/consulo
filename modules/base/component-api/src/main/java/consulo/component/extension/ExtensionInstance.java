/*
 * Copyright 2013-2023 consulo.io
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
package consulo.component.extension;

import consulo.component.internal.ExtensionInstanceRef;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 26/01/2023
 */
public final class ExtensionInstance<T> implements Supplier<T> {
  @Nonnull
  @SuppressWarnings("unchecked")
  public static <K> Supplier<K> current() {
    // since static fields init before new () - we need register this instance to thread local
    // and factory will set instance value
    // another method will be like
    // Class caller = getCallerClass()
    // Application.getExtensionPoint(ApiClass).getExtensionImpl(caller)
    ExtensionInstanceRef ref = ExtensionInstanceRef.CURRENT_CREATION.get();
    if (ref == null) {
      throw new IllegalArgumentException("Illegal call. Require init inside extension creation");
    }

    ExtensionInstance<K> instance = new ExtensionInstance<>();
    ref.setter = o -> instance.myValue = (K)o;
    return instance;
  }

  private T myValue;

  private ExtensionInstance() {
  }

  @Override
  public T get() {
    return Objects.requireNonNull(myValue);
  }
}
