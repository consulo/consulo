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
package consulo.ide.impl.idea.openapi.module.impl;

import consulo.project.Project;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.util.dataholder.UserDataHolderBase;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

public class ModuleConfigurationStateImpl extends UserDataHolderBase implements ModuleConfigurationState {
  private final ModulesConfigurator myProvider;
  private final LibrariesConfigurator myLibrariesConfigurator;
  private final Project myProject;
  @Nonnull
  private final Supplier<? extends ModifiableRootModel> myModifiableRootModelValue;

  public ModuleConfigurationStateImpl(@Nonnull Project project,
                                      @Nonnull ModulesConfigurator provider,
                                      @Nonnull LibrariesConfigurator librariesConfigurator,
                                      @Nonnull Supplier<? extends ModifiableRootModel> modifiableRootModelValue) {
    myProvider = provider;
    myLibrariesConfigurator = librariesConfigurator;
    myProject = project;
    myModifiableRootModelValue = modifiableRootModelValue;
  }

  @Nonnull
  @Override
  public ModulesConfigurator getModulesConfigurator() {
    return myProvider;
  }

  @Nonnull
  @Override
  public LibrariesConfigurator getLibrariesConfigurator() {
    return myLibrariesConfigurator;
  }

  @Override
  @Nullable
  public ModifiableRootModel getRootModel() {
    return myModifiableRootModelValue.get();
  }

  @Override
  public Project getProject() {
    return myProject;
  }
}
