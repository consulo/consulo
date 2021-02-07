/*
 * Copyright 2013-2021 consulo.io
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
package consulo.util.collection;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public final class Lists {
  @Nonnull
  @Contract(pure = true)
  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<T>();
  }

  @Nonnull
  @Contract(pure = true)
  @SafeVarargs
  public static <T> ArrayList<T> newArrayList(@Nonnull T... elements) {
    ArrayList<T> list = newArrayListWithCapacity(elements.length);
    Collections.addAll(list, elements);
    return list;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
    return new ArrayList<T>(size);
  }
}
