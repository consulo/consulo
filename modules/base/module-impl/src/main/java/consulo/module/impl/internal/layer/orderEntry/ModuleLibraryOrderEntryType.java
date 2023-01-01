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
package consulo.module.impl.internal.layer.orderEntry;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.library.Library;
import consulo.logging.Logger;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.module.impl.internal.layer.library.LibraryTableImplUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
@ExtensionImpl
public class ModuleLibraryOrderEntryType implements OrderEntryType<ModuleLibraryOrderEntryImpl> {
  public static final String ID = "module-library";

  private static final Logger LOG = Logger.getInstance(ModuleLibraryOrderEntryType.class);

  @Nonnull
  public static ModuleLibraryOrderEntryType getInstance() {
    return EP_NAME.findExtensionOrFail(ModuleLibraryOrderEntryType.class);
  }

  public static final String EXPORTED_ATTR = "exported";

  @Nonnull
  @Override
  public String getId() {
    return ID;
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
      LOG.error("Exception while writing module library: " + orderEntry.getLibraryName() + " in module: " + orderEntry.getOwnerModule().getName(), e);
    }
  }
}
