/*
 * Copyright 2013-2016 consulo.io
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
package consulo.util.pointers;

import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 22.06.14
 */
public class NamedPointerUtil {
  @Nullable
  public static <T extends Named> T get(@Nullable NamedPointer<T> pointer) {
    return pointer == null ? null : pointer.get();
  }

  @Nullable
  public static <T extends Named> String getName(@Nullable NamedPointer<T> getter) {
    return getter == null ? null : getter.getName();
  }
}
