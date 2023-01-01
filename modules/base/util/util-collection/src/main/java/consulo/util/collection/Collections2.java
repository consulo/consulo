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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 30/01/2022
 */
public class Collections2 {
  /**
   * Create collection from iterable. Optimized version if iterable is collection
   */
  @Nonnull
  @SuppressWarnings("unchecked")
  public static <C extends Collection<E>, E> C of(@Nonnull Iterable<E> iterable, @Nonnull Function<Collection<E>, C> factory) {
    if (iterable instanceof Collection) {
      return (C)factory.apply((Collection)iterable);
    }

    C emptyCollection = factory.apply(List.of());
    for (E e : iterable) {
      emptyCollection.add(e);
    }
    return emptyCollection;
  }
}
