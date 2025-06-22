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
package consulo.language.psi;

import consulo.application.Application;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * TODO rewrite using ExtensionCacheKey
 *
 * @author VISTALL
 * @since 2013-04-02
 */
public class ElementTypeEntryExtensionCollector<E extends Predicate<IElementType>> {
  @Nonnull
  public static <E extends Predicate<IElementType>> ElementTypeEntryExtensionCollector<E> create(@Nonnull Class<E> clazz) {
    return new ElementTypeEntryExtensionCollector<>(clazz);
  }

  private final Class<E> myExtensionClass;

  private ElementTypeEntryExtensionCollector(@Nonnull Class<E> clazz) {
    myExtensionClass = clazz;
  }

  private final Map<IElementType, E> myMap = new ConcurrentHashMap<>();

  @Nonnull
  public E getValue(@Nonnull IElementType elementType) {
    return myMap.computeIfAbsent(elementType, it -> {
      E factory = Application.get().getExtensionPoint(myExtensionClass).findFirstSafe(e -> e.test(elementType));
      if (factory == null) {
        throw new IllegalArgumentException("ElementType " + it + " is not handled in " + myExtensionClass);
      }
      return factory;
    });
  }
}
