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

package consulo.module.content.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.pointer.NamedPointer;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkPointerManager;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.ModulePointerManager;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class RootConfigurationAccessor {
  @Nullable
  public Library getLibrary(Library library, String libraryName, String libraryLevel) {
    return library;
  }

  @Nullable
  public Sdk getSdk(Sdk sdk, String sdkName) {
    return sdk;
  }

  @Nonnull
  public NamedPointer<Sdk> getSdkPointer(String sdkName) {
    return SdkPointerManager.getInstance().create(sdkName);
  }

  public Module getModule(Module module, String moduleName) {
    return module;
  }

  @Nonnull
  @RequiredReadAction
  public NamedPointer<Module> getModulePointer(Project project, String name) {
    return ModulePointerManager.getInstance(project).create(name);
  }
}