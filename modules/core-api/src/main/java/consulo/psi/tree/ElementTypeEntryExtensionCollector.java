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
import com.intellij.util.containers.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 1:53/02.04.13
 */
public class ElementTypeEntryExtensionCollector<E extends Predicate<IElementType>> {
  public static <E extends Predicate<IElementType>> ElementTypeEntryExtensionCollector<E> create(@NotNull String epName) {
    return new ElementTypeEntryExtensionCollector<E>(epName);
  }

  private final ExtensionPointName<E> myExtensionPointName;

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<IElementType, E> myMap = new ConcurrentFactoryMap<IElementType, E>() {
    @Nullable
    @Override
    protected E create(IElementType elementType) {
      E factory = null;
      for (E e : myExtensionPointName.getExtensions()) {
        if (e.apply(elementType)) {
          factory = e;
          break;
        }
      }
      if (factory == null) {
        throw new IllegalArgumentException("ElementType " + elementType + " is not handled in " + myExtensionPointName);
      }
      return factory;
    }
  };

  private ElementTypeEntryExtensionCollector(@NotNull String epName) {
    myExtensionPointName = ExtensionPointName.create(epName);
  }

  @NotNull
  public E getValue(@NotNull IElementType elementType) {
    return myMap.get(elementType);
  }

  @NotNull
  public ExtensionPointName<E> getExtensionPointName() {
    return myExtensionPointName;
  }
}
