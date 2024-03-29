/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.module.Module;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.util.OrderEntryUtil;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class PlaceInModuleClasspath extends PlaceInProjectStructure {
  private final Module myModule;
  private final ProjectStructureElement myElement;
  private final OrderEntry myOrderEntry;
  private final ModulesConfigurator myModulesConfigurator;

  public PlaceInModuleClasspath(ModulesConfigurator modulesConfigurator, Module module, ProjectStructureElement element, OrderEntry orderEntry) {
    myModulesConfigurator = modulesConfigurator;
    myModule = module;
    myElement = element;
    myOrderEntry = orderEntry;
  }

  public PlaceInModuleClasspath(@Nonnull ModulesConfigurator configurator, @Nonnull Module module, ProjectStructureElement element, @Nonnull ProjectStructureElement elementInClasspath) {
    myModulesConfigurator = configurator;
    myModule = module;
    myElement = element;
    ModuleRootModel rootModel = myModulesConfigurator.getRootModel(myModule);
    if (elementInClasspath instanceof LibraryProjectStructureElement) {
      myOrderEntry = OrderEntryUtil.findLibraryOrderEntry(rootModel, ((LibraryProjectStructureElement)elementInClasspath).getLibrary());
    }
    else if (elementInClasspath instanceof ModuleProjectStructureElement) {
      myOrderEntry = OrderEntryUtil.findModuleOrderEntry(rootModel, ((ModuleProjectStructureElement)elementInClasspath).getModule());
    }
    else if (elementInClasspath instanceof SdkProjectStructureElement) {
      myOrderEntry = OrderEntryUtil.findJdkOrderEntry(rootModel, ((SdkProjectStructureElement)elementInClasspath).getSdk());
    }
    else {
      myOrderEntry = null;
    }
  }

  @Nonnull
  @Override
  public ProjectStructureElement getContainingElement() {
    return myElement;
  }

  @Override
  public String getPlacePath() {
    return myOrderEntry != null ? myOrderEntry.getPresentableName() : null;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> navigate(@Nonnull Project project) {
    ShowSettingsUtil showSettingsUtil = ShowSettingsUtil.getInstance();
    return showSettingsUtil.showProjectStructureDialog(project, projectStructureSelector -> {
      projectStructureSelector.selectOrderEntry(myModule, myOrderEntry);
    });
  }
}
