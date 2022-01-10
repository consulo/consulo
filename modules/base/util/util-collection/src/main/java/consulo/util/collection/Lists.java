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
package consulo.util.collection;

import consulo.util.collection.impl.list.LockFreeCopyOnWriteArrayList;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 10/01/2022
 */
public final class Lists {
  /**
   * Creates List which is thread-safe to modify and iterate.
   * It differs from the java.util.concurrent.CopyOnWriteArrayList in the following:
   * - faster modification in the uncontended case
   * - less memory
   * - slower modification in highly contented case (which is the kind of situation you shouldn't use COWAL anyway)
   * <p/>
   * N.B. Avoid using {@code list.toArray(new T[list.size()])} on this list because it is inherently racey and
   * therefore can return array with null elements at the end.
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> newLockFreeCopyOnWriteList() {
    return new LockFreeCopyOnWriteArrayList<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> newLockFreeCopyOnWriteList(@Nonnull Collection<? extends T> c) {
    return new LockFreeCopyOnWriteArrayList<T>(c);
  }
}
