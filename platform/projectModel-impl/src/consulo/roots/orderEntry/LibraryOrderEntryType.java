/*
 * Copyright 2013-2014 must-be.org
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
package consulo.roots.orderEntry;

import com.intellij.openapi.roots.DependencyScope;
import consulo.roots.ModuleRootLayer;
import com.intellij.openapi.roots.impl.LibraryOrderEntryImpl;
import com.intellij.openapi.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.InvalidDataException;
import consulo.lombok.annotations.Lazy;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class LibraryOrderEntryType implements OrderEntryType<LibraryOrderEntryImpl> {
  @NotNull
  @Lazy
  public static LibraryOrderEntryType getInstance() {
    return EP_NAME.findExtension(LibraryOrderEntryType.class);
  }

  @NonNls
  private static final String NAME_ATTR = "name";
  @NonNls
  private static final String LEVEL_ATTR = "level";
  @NonNls
  private static final String EXPORTED_ATTR = "exploded";

  @NotNull
  @Override
  public String getId() {
    return "library";
  }

  @NotNull
  @Override
  public LibraryOrderEntryImpl loadOrderEntry(@NotNull Element element, @NotNull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    String name = element.getAttributeValue(NAME_ATTR);
    if (name == null) {
      throw new InvalidDataException();
    }

    String level = element.getAttributeValue(LEVEL_ATTR, LibraryTablesRegistrar.PROJECT_LEVEL);
    DependencyScope dependencyScope = DependencyScope.readExternal(element);
    boolean exported = element.getAttributeValue(EXPORTED_ATTR) != null;
    return new LibraryOrderEntryImpl(name, level, (ModuleRootLayerImpl)moduleRootLayer, dependencyScope, exported, false);
  }

  @Override
  public void storeOrderEntry(@NotNull Element element, @NotNull LibraryOrderEntryImpl orderEntry) {
    final String libraryLevel = orderEntry.getLibraryLevel();
    if (orderEntry.isExported()) {
      element.setAttribute(EXPORTED_ATTR, "");
    }
    orderEntry.getScope().writeExternal(element);
    element.setAttribute(NAME_ATTR, orderEntry.getLibraryName());
    element.setAttribute(LEVEL_ATTR, libraryLevel);
  }
}
