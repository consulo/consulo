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

import consulo.util.lang.lazy.ClearableLazyValue;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 31/01/2022
 */
public class ClearableNullableLazyValueImpl<T> extends NullableLazyValueImpl<T> implements ClearableLazyValue<T> {
  public ClearableNullableLazyValueImpl(Supplier<T> factory) {
    super(factory);
  }

  @Override
  public void clear() {
    myValue = null;
    myComputed = false;
  }
}
