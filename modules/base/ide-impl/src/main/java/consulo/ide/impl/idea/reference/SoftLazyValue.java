/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.reference;

import jakarta.annotation.Nonnull;

import java.lang.ref.SoftReference;

/**
 * @author Dmitry Avdeev
 * @since 25.05.2012
 */
public abstract class SoftLazyValue<T> {
  private SoftReference<T> myReference;

  public T getValue() {
    T t;
    if (myReference == null || (t = myReference.get()) == null) {
      t = compute();
      myReference = new SoftReference<T>(t);
    }
    return t;
  }

  @Nonnull
  protected abstract T compute();
}
