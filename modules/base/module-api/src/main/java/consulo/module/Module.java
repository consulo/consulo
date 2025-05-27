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
package consulo.module;

import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.util.pointer.Named;
import consulo.disposer.Disposable;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.util.collection.ArrayFactory;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a module in an Consulo project.
 *
 * @see ModuleManager#getModules()
 */
public interface Module extends ComponentManager, Disposable, Named {
  Key<Module> KEY = Key.create(Module.class);

  Module[] EMPTY_ARRAY = new Module[0];

  ArrayFactory<Module> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new Module[count];

  /**
   * Returns the <code>VirtualFile</code> to the module dir
   *
   * @return the virtual file instance.
   */
  @Nullable
  VirtualFile getModuleDir();

  /**
   * Returns the path to the module dir
   *
   * @return the path to the module dir
   */
  @Nullable
  String getModuleDirPath();

  /**
   * Returns the path to the module url
   *
   * @return the path to the module url
   */
  @Nullable
  String getModuleDirUrl();

  /**
   * Returns the project to which this module belongs.
   *
   * @return the project instance.
   */
  @Nonnull
  Project getProject();

  @Nonnull
  default Application getApplication() {
    return getProject().getApplication();
  }

  /**
   * Returns the name of this module.
   *
   * @return the module name.
   */
  @Override
  @Nonnull
  String getName();

  /**
   * Checks if the module instance has been disposed and unloaded.
   *
   * @return true if the module has been disposed, false otherwise
   */
  @Override
  boolean isDisposed();

  @Nullable
  <T extends ModuleExtension<T>> T getExtension(@Nonnull Class<T> clazz);

  @Nullable
  <T extends ModuleExtension<T>> T getExtension(@Nonnull String key);
}
