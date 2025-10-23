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
package consulo.module.content.layer;

import consulo.annotation.access.RequiredWriteAction;
import consulo.module.content.ModuleRootManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Model of roots that should be used by clients to modify module roots.
 *
 * @author dsl
 * @see ModuleRootManager#getModifiableModel()
 */
public interface ModifiableRootModel extends ModuleRootModel, ModifiableModuleRootLayer {
  String DEFAULT_LAYER_NAME = "Default";

  @Nonnull
  ModifiableModuleRootLayer addLayer(@Nonnull String name, @Nullable String nameForCopy, boolean activate);

  boolean removeLayer(@Nonnull String name, boolean initDefault);

  void removeAllLayers(boolean initDefault);

  @Nullable()
  ModifiableModuleRootLayer setCurrentLayer(@Nonnull String name);

  void clear();

  /**
   * Commits changes to a <code>{@link ModuleRootManager}</code>.
   * After <code>commit()<code>, the model becomes read-only.
   */
  @RequiredWriteAction
  void commit();

  boolean isChanged();

  /**
   * Must be invoked for uncommitted models that are no longer needed.
   */
  void dispose();

  boolean isWritable();

  boolean isDisposed();
}
