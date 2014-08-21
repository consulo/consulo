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
package org.mustbe.consulo.roots.impl;

import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.roots.impl.ModuleLibraryOrderEntryImpl;
import com.intellij.openapi.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.types.BinariesOrderRootType;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.FileAppearanceService;
import com.intellij.openapi.roots.ui.OrderEntryAppearanceService;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.classpath.LibraryClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.consulo.lombok.annotations.LazyInstance;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
@Logger
public class ModuleLibraryOrderEntryTypeProvider implements OrderEntryTypeProviderEx<ModuleLibraryOrderEntryImpl> {
  @NotNull
  @LazyInstance
  public static ModuleLibraryOrderEntryTypeProvider getInstance() {
    return EP_NAME.findExtension(ModuleLibraryOrderEntryTypeProvider.class);
  }

  @NonNls
  public static final String EXPORTED_ATTR = "exported";

  @NotNull
  @Override
  public String getId() {
    return "module-library";
  }

  @NotNull
  @Override
  public ModuleLibraryOrderEntryImpl loadOrderEntry(@NotNull Element element, @NotNull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    boolean exported = element.getAttributeValue(EXPORTED_ATTR) != null;
    DependencyScope scope = DependencyScope.readExternal(element);
    Library library = LibraryTableImplUtil.loadLibrary(element, (ModuleRootLayerImpl)moduleRootLayer);
    return new ModuleLibraryOrderEntryImpl(library, (ModuleRootLayerImpl)moduleRootLayer, exported, scope, false);
  }

  @Override
  public void storeOrderEntry(@NotNull Element element, @NotNull ModuleLibraryOrderEntryImpl orderEntry) {
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
      LOGGER.error("Exception while writing module library: " + orderEntry.getLibraryName() + " in module: " + orderEntry.getOwnerModule().getName(), e);
    }
  }

  @NotNull
  @Override
  public ClasspathTableItem<ModuleLibraryOrderEntryImpl> createTableItem(@NotNull ModuleLibraryOrderEntryImpl orderEntry, @NotNull StructureConfigurableContext context) {
    return new LibraryClasspathTableItem<ModuleLibraryOrderEntryImpl>(orderEntry, context);
  }

  @NotNull
  @Override
  public CellAppearanceEx getCellAppearance(@NotNull ModuleLibraryOrderEntryImpl orderEntry) {
    if (!orderEntry.isValid()) { //library can be removed
      return FileAppearanceService.getInstance().forInvalidUrl(orderEntry.getPresentableName());
    }
    Library library = orderEntry.getLibrary();
    assert library != null : orderEntry;
    return OrderEntryAppearanceService.getInstance().forLibrary(orderEntry.getModuleRootLayer().getProject(), library,
                                                                !((LibraryEx)library).getInvalidRootUrls(BinariesOrderRootType.getInstance()).isEmpty());
  }
}
