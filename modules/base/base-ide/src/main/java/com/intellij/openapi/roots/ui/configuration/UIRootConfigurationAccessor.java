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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.roots.impl.RootConfigurationAccessor;
import com.intellij.openapi.roots.libraries.Library;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.settings.impl.ProjectStructureSettingsUtil;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.util.pointers.NamedPointer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  @Nullable
  public Library getLibrary(Library library, final String libraryName, final String libraryLevel) {
    if (library == null) {
      if (libraryName != null) {
        library = myLibrariesConfigurator.getLibrary(libraryName, libraryLevel);
      }
    }
    else {
      final Library model = myLibrariesConfigurator.getLibraryModel(library);
      if (model != null) {
        library = model;
      }
      library = myLibrariesConfigurator.getLibrary(library.getName(), library.getTable().getTableLevel());
    }
    return library;
  }

  @Override
  @Nullable
  public Sdk getSdk(final Sdk sdk, final String sdkName) {
    final SdkModel model = ((ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance()).getSdksModel();
    return sdkName != null ? model.findSdk(sdkName) : sdk;
  }

  @Nonnull
  @Override
  public NamedPointer<Sdk> getSdkPointer(String sdkName) {
    return new NamedPointer<>() {
      @Nullable
      @Override
      public Sdk get() {
        return ((ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance()).getSdksModel().findSdk(sdkName);
      }

      @Nonnull
      @Override
      public String getName() {
        return sdkName;
      }
    };
  }

  @Override
  public Module getModule(final Module module, final String moduleName) {
    if (module == null) {
      return myModulesConfigurator.getModule(moduleName);
    }
    return module;
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public NamedPointer<Module> getModulePointer(Project project, String name) {
    return new NamedPointer<>() {
      @Nullable
      @Override
      public Module get() {
        return myModulesConfigurator.getModule(name);
      }

      @Nonnull
      @Override
      public String getName() {
        return name;
      }
    };
  }
}
