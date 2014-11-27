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

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.roots.impl.LibraryOrderEntryImpl;
import com.intellij.openapi.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.types.BinariesOrderRootType;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.FileAppearanceService;
import com.intellij.openapi.roots.ui.OrderEntryAppearanceService;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.classpath.LibraryClasspathTableItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.util.InvalidDataException;
import org.consulo.lombok.annotations.LazyInstance;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class LibraryOrderEntryTypeProvider extends OrderEntryTypeProviderEx<LibraryOrderEntryImpl> {
  @NotNull
  @LazyInstance
  public static LibraryOrderEntryTypeProvider getInstance() {
    return EP_NAME.findExtension(LibraryOrderEntryTypeProvider.class);
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

  @Override
  public void navigate(@NotNull final LibraryOrderEntryImpl orderEntry) {
    Project project = orderEntry.getModuleRootLayer().getProject();
    final ProjectStructureConfigurable config = ProjectStructureConfigurable.getInstance(project);
    ShowSettingsUtil.getInstance().editConfigurable(project, config, new Runnable() {
      @Override
      public void run() {
        config.select(orderEntry, true);
      }
    });
  }

  @NotNull
  @Override
  public CellAppearanceEx getCellAppearance(@NotNull LibraryOrderEntryImpl orderEntry) {
    if (!orderEntry.isValid()) { //library can be removed
      return FileAppearanceService.getInstance().forInvalidUrl(orderEntry.getPresentableName());
    }
    Library library = orderEntry.getLibrary();
    assert library != null : orderEntry;
    return OrderEntryAppearanceService.getInstance().forLibrary(orderEntry.getModuleRootLayer().getProject(), library,
                                                                !((LibraryEx)library).getInvalidRootUrls(BinariesOrderRootType.getInstance()).isEmpty());
  }

  @NotNull
  @Override
  public ClasspathTableItem<LibraryOrderEntryImpl> createTableItem(@NotNull LibraryOrderEntryImpl orderEntry, @NotNull StructureConfigurableContext context) {
    return new LibraryClasspathTableItem<LibraryOrderEntryImpl>(orderEntry, context);
  }
}
