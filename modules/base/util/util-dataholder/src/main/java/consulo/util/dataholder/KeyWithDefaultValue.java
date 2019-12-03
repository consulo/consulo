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

package consulo.util.dataholder;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author peter
 */
public final class KeyWithDefaultValue<T> extends Key<T> {
  @Nonnull
  public static <T> KeyWithDefaultValue<T> create(@Nonnull String name, final T defValue) {
    return new KeyWithDefaultValue<T>(name, () -> defValue);
  }

  @Nonnull
  public static <T> KeyWithDefaultValue<T> create(@Nonnull String name, final Supplier<? extends T> supplier) {
    return new KeyWithDefaultValue<T>(name, supplier);
  }

  @Nonnull
  private final Supplier<? extends T> myValueGetter;

  @SuppressWarnings("deprecation")
  private KeyWithDefaultValue(@Nonnull String name, @Nonnull Supplier<? extends T> valueGetter) {
    super(name);
    myValueGetter = valueGetter;
  }

  public T getDefaultValue() {
    return myValueGetter.get();
  }
}
