/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.pointer.NamedPointer;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModel;
import consulo.content.library.Library;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.module.Module;
import consulo.module.content.internal.RootConfigurationAccessor;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * @author yole
 */
public class UIRootConfigurationAccessor extends RootConfigurationAccessor {
  private final ModulesConfigurator myModulesConfigurator;
  private final LibrariesConfigurator myLibrariesConfigurator;

  public UIRootConfigurationAccessor(ModulesConfigurator modulesConfigurator, LibrariesConfigurator librariesConfigurator) {
    myModulesConfigurator = modulesConfigurator;
    myLibrariesConfigurator = librariesConfigurator;
  }

  @Override
  public @Nullable Library getLibrary(@Nullable Library library, @Nullable String libraryName, @Nullable String libraryLevel) {
    if (library == null) {
      if (libraryName != null) {
        library = myLibrariesConfigurator.getLibrary(libraryName, libraryLevel);
      }
    }
    else {
      Library model = myLibrariesConfigurator.getLibraryModel(library);
      if (model != null) {
        library = model;
      }
      library = myLibrariesConfigurator.getLibrary(library.getName(), library.getTable().getTableLevel());
    }
    return library;
  }

  @Override
  public @Nullable Sdk getSdk(Sdk sdk, @Nullable String sdkName) {
    SdkModel model = ShowSettingsUtil.getInstance().getSdksModel();
    return sdkName != null ? model.findSdk(sdkName) : sdk;
  }

  @Override
  public NamedPointer<Sdk> getSdkPointer(String sdkName) {
    return new NamedPointer<>() {
      @Override
      public @Nullable Sdk get() {
        return ShowSettingsUtil.getInstance().getSdksModel().findSdk(sdkName);
      }

      @Override
      public String getName() {
        return sdkName;
      }
    };
  }

  @Override
  @RequiredReadAction
  public Module getModule(@Nullable Module module, String moduleName) {
    if (module == null) {
      return myModulesConfigurator.getModule(moduleName);
    }
    return module;
  }

  @Override
  @RequiredReadAction
  public NamedPointer<Module> getModulePointer(Project project, String name) {
    return new NamedPointer<>() {
      @Override
      @RequiredReadAction
      public @Nullable Module get() {
        return myModulesConfigurator.getModule(name);
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }
}
