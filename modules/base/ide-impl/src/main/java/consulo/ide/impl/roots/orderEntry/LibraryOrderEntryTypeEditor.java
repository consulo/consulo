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

import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.ide.setting.module.ClasspathTableItem;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath.LibraryClasspathTableItem;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ide.setting.module.OrderEntryTypeEditor;
import consulo.ide.ui.FileAppearanceService;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.module.impl.internal.layer.orderEntry.LibraryOrderEntryImpl;
import consulo.module.impl.internal.layer.orderEntry.LibraryOrderEntryType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
@ExtensionImpl
public class LibraryOrderEntryTypeEditor implements OrderEntryTypeEditor<LibraryOrderEntryImpl> {
  @RequiredUIAccess
  @Override
  public void navigate(@Nonnull final LibraryOrderEntryImpl orderEntry) {
    Project project = orderEntry.getModuleRootLayer().getProject();
    ShowSettingsUtil.getInstance().showProjectStructureDialog(project, config -> config.select(orderEntry, true));
  }

  @Nonnull
  @Override
  public String getOrderTypeId() {
    return LibraryOrderEntryType.ID;
  }

  @Nonnull
  @Override
  public Consumer<ColoredTextContainer> getRender(@Nonnull LibraryOrderEntryImpl orderEntry) {
    if (!orderEntry.isValid()) { //library can be removed
      return FileAppearanceService.getInstance().getRenderForInvalidUrl(orderEntry.getPresentableName());
    }
    Library library = orderEntry.getLibrary();
    assert library != null : orderEntry;
    return OrderEntryAppearanceService.getInstance()
            .getRenderForLibrary(orderEntry.getModuleRootLayer().getProject(), library, !((LibraryEx)library).getInvalidRootUrls(BinariesOrderRootType.getInstance()).isEmpty());

  }

  @Nonnull
  @Override
  public ClasspathTableItem<LibraryOrderEntryImpl> createTableItem(@Nonnull LibraryOrderEntryImpl orderEntry,
                                                                   @Nonnull Project project,
                                                                   @Nonnull ModulesConfigurator modulesConfigurator,
                                                                   @Nonnull LibrariesConfigurator librariesConfigurator) {
    return new LibraryClasspathTableItem<>(orderEntry, project, modulesConfigurator, librariesConfigurator);
  }
}
