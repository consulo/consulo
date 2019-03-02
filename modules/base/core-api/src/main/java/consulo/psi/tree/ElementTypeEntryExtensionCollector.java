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
package consulo.psi.tree;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ConcurrentFactoryMap;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 1:53/02.04.13
 */
public class ElementTypeEntryExtensionCollector<E extends Predicate<IElementType>> {
  @Nonnull
  public static <E extends Predicate<IElementType>> ElementTypeEntryExtensionCollector<E> create(@Nonnull String epName) {
    return new ElementTypeEntryExtensionCollector<>(epName);
  }

  private ExtensionPointName<E> myExtensionPointName;

  private ElementTypeEntryExtensionCollector(@Nonnull String epName) {
    myExtensionPointName = ExtensionPointName.create(epName);
  }

  private final Map<IElementType, E> myMap = ConcurrentFactoryMap.createMap(elementType -> {
    E factory = null;
    for (E e : myExtensionPointName.getExtensionList()) {
      if (e.test(elementType)) {
        factory = e;
        break;
      }
    }
    if (factory == null) {
      throw new IllegalArgumentException("ElementType " + elementType + " is not handled in " + myExtensionPointName);
    }
    return factory;
  });

  @Nonnull
  public E getValue(@Nonnull IElementType elementType) {
    return myMap.get(elementType);
  }

  @Nonnull
  public ExtensionPointName<E> getExtensionPointName() {
    return myExtensionPointName;
  }
}
