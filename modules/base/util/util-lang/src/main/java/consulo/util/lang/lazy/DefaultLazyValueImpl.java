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

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 03/01/2022
 */
class DefaultLazyValueImpl<T> implements LazyValue<T> {
  private final Supplier<T> myFactory;

  protected volatile T myValue;

  DefaultLazyValueImpl(Supplier<T> factory) {
    myFactory = factory;
  }

  @Nonnull
  @Override
  public T get() {
    T result = myValue;
    if (result == null) {
      result = myFactory.get();
      myValue = result;
    }
    return result;
  }
}
