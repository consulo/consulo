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
package consulo.disposer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2023-11-05
 */
public interface AutoDisposable extends Disposable, AutoCloseable {
  @Nonnull
  static AutoDisposable newAutoDisposable() {
    return newAutoDisposable(null);
  }

  @Nonnull
  static AutoDisposable newAutoDisposable(@Nullable String debugName) {
    return new AutoDisposable() {
      @Override
      public void dispose() {

      }

      @Override
      public String toString() {
        return debugName == null ? super.toString() : debugName;
      }
    };
  }

  @Override
  default void close() {
    disposeWithTree();
  }
}
