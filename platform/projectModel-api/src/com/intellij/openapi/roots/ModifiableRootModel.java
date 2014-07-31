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
package com.intellij.openapi.roots;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Model of roots that should be used by clients to modify module roots.
 *
 * @author dsl
 * @see ModuleRootManager#getModifiableModel()
 */
public interface ModifiableRootModel extends ModuleRootModel, ModifiableModuleRootLayer {
  @Nullable
  ModifiableModuleRootLayer addLayer(@NotNull String name, @Nullable String nameForCopy, boolean activate);

  @Nullable
  ModifiableModuleRootLayer removeLayer(@NotNull String name, boolean initDefault);

  @Nullable()
  ModifiableModuleRootLayer setCurrentLayer(@NotNull String name);

  @NotNull
  Project getProject();

  void clear();

  /**
   * Commits changes to a <code>{@link ModuleRootManager}</code>.
   * Should be invoked in a write action. After <code>commit()<code>, the model
   * becomes read-only.
   */
  void commit();

  /**
   * Must be invoked for uncommited models that are no longer needed.
   */
  void dispose();

  boolean isWritable();

  boolean isDisposed();
}
