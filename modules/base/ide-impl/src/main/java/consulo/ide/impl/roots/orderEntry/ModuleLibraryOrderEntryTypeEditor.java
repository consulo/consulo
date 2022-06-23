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
package consulo.ide.impl.roots.orderEntry;

import consulo.content.base.BinariesOrderRootType;
import consulo.content.impl.internal.library.LibraryEx;
import consulo.content.library.Library;
import consulo.ide.setting.module.ClasspathTableItem;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath.LibraryClasspathTableItem;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.ide.ui.FileAppearanceService;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.module.impl.internal.layer.orderEntry.ModuleLibraryOrderEntryImpl;
import consulo.module.impl.internal.layer.orderEntry.ModuleLibraryOrderEntryType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public class ModuleLibraryOrderEntryTypeEditor implements OrderEntryTypeEditor<ModuleLibraryOrderEntryImpl> {
  private final Provider<FileAppearanceService> myFileAppearanceService;
  private final Provider<OrderEntryAppearanceService> myOrderEntryAppearanceService;

  @Inject
  public ModuleLibraryOrderEntryTypeEditor(Provider<FileAppearanceService> fileAppearanceService, Provider<OrderEntryAppearanceService> orderEntryAppearanceService) {
    myFileAppearanceService = fileAppearanceService;
    myOrderEntryAppearanceService = orderEntryAppearanceService;
  }

  @RequiredUIAccess
  @Override
  public void navigate(@Nonnull final ModuleLibraryOrderEntryImpl orderEntry) {
    Project project = orderEntry.getModuleRootLayer().getProject();
    ShowSettingsUtil.getInstance().showProjectStructureDialog(project, config -> config.select(orderEntry, true));
  }

  @Nonnull
  @Override
  public ClasspathTableItem<ModuleLibraryOrderEntryImpl> createTableItem(@Nonnull ModuleLibraryOrderEntryImpl orderEntry,
                                                                         @Nonnull Project project,
                                                                         @Nonnull ModulesConfigurator modulesConfigurator,
                                                                         @Nonnull LibrariesConfigurator librariesConfigurator) {
    return new LibraryClasspathTableItem<>(orderEntry, project, modulesConfigurator, librariesConfigurator);
  }

  @Nonnull
  @Override
  public String getOrderTypeId() {
    return ModuleLibraryOrderEntryType.ID;
  }

  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRender(@Nonnull ModuleLibraryOrderEntryImpl orderEntry) {
    if (!orderEntry.isValid()) {
      return myFileAppearanceService.get().getRenderForInvalidUrl(orderEntry.getPresentableName());
    }

    Library library = orderEntry.getLibrary();
    assert library != null : orderEntry;
    return myOrderEntryAppearanceService.get()
            .getRenderForLibrary(orderEntry.getModuleRootLayer().getProject(), library, !((LibraryEx)library).getInvalidRootUrls(BinariesOrderRootType.getInstance()).isEmpty());
  }
}
