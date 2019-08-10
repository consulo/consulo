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
package consulo.roots.orderEntry;

import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import consulo.logging.Logger;
import consulo.roots.ModuleRootLayer;
import consulo.roots.impl.ModuleRootLayerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class ModuleLibraryOrderEntryType implements OrderEntryType<ModuleLibraryOrderEntryImpl> {
  public static final Logger LOGGER = Logger.getInstance(ModuleLibraryOrderEntryType.class);

  @Nonnull
  public static ModuleLibraryOrderEntryType getInstance() {
    return EP_NAME.findExtension(ModuleLibraryOrderEntryType.class);
  }

  @NonNls
  public static final String EXPORTED_ATTR = "exported";

  @Nonnull
  @Override
  public String getId() {
    return "module-library";
  }

  @Nonnull
  @Override
  public ModuleLibraryOrderEntryImpl loadOrderEntry(@Nonnull Element element, @Nonnull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    boolean exported = element.getAttributeValue(EXPORTED_ATTR) != null;
    DependencyScope scope = DependencyScope.readExternal(element);
    Library library = LibraryTableImplUtil.loadLibrary(element, (ModuleRootLayerImpl)moduleRootLayer);
    return new ModuleLibraryOrderEntryImpl(library, (ModuleRootLayerImpl)moduleRootLayer, exported, scope, false);
  }

  @Override
  public void storeOrderEntry(@Nonnull Element element, @Nonnull ModuleLibraryOrderEntryImpl orderEntry) {
    if (orderEntry.isExported()) {
      element.setAttribute(EXPORTED_ATTR, "");
    }
    orderEntry.getScope().writeExternal(element);
    try {
      Library library = orderEntry.getLibrary();
      assert library != null;
      library.writeExternal(element);
    }
    catch (WriteExternalException e) {
      ModuleLibraryOrderEntryType
              .LOGGER.error("Exception while writing module library: " + orderEntry.getLibraryName() + " in module: " + orderEntry.getOwnerModule().getName(), e);
    }
  }
}
