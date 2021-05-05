/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import javax.annotation.Nullable;
import java.util.Objects;

public interface UserDataHolder {
  @Nullable
  <T> T getUserData(@Nonnull Key<T> key);

  <T> void putUserData(@Nonnull Key<T> key, @Nullable T value);

  /**
   * @return written value
   */
  @Nonnull
  default <T> T putUserDataIfAbsent(@Nonnull Key<T> key, @Nonnull T value) {
    T oldValue = getUserData(key);
    if (oldValue == null) {
      putUserData(key, value);
      return value;
    }
    else {
      return oldValue;
    }
  }

  /**
   * Replaces (atomically) old value in the map with the new one
   *
   * @return true if old value got replaced, false otherwise
   * @see ConcurrentMap#replace(Object, Object, Object)
   */
  default <T> boolean replace(@Nonnull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
    T data = getUserData(key);

    if (!Objects.equals(data, oldValue)) {
      return false;
    }

    putUserData(key, newValue);
    return true;
  }
}