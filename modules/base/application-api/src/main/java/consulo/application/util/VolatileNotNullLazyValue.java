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

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@Deprecated
@DeprecationInfo("Use LazyValue")
public abstract class VolatileNotNullLazyValue<T> extends NotNullLazyValue<T> {

  private volatile T myValue;

  @Nonnull
  public final T getValue() {
    T value = myValue;
    if (value != null) {
      return value;
    }
    value = myValue = compute();
    return value;
  }
}