/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;

/**
 * @author Gregory.Shrago
 */
public interface FilePropertyPusher<T> {
  ExtensionPointName<FilePropertyPusher<?>> EP_NAME = ExtensionPointName.create("com.intellij.filePropertyPusher");

  default void initExtra(@Nonnull Project project, @Nonnull MessageBus bus) {
  }

  /**
   * @deprecated use {@link FilePropertyPusher#initExtra(Project, MessageBus)} instead
   */
  //@ApiStatus.ScheduledForRemoval(inVersion = "2021.3")
  @Deprecated
  @SuppressWarnings("unused")
  default void initExtra(@Nonnull Project project, @Nonnull MessageBus bus, @Nonnull Engine languageLevelUpdater) {
    initExtra(project, bus);
  }

  @Nonnull
  Key<T> getFileDataKey();

  boolean pushDirectoriesOnly();

  @Nonnull
  T getDefaultValue();

  @Nullable
  T getImmediateValue(@Nonnull Project project, @Nullable VirtualFile file);

  @Nullable
  T getImmediateValue(@Nonnull Module module);

  default boolean acceptsFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    return acceptsFile(file);
  }

  boolean acceptsFile(@Nonnull VirtualFile file);

  boolean acceptsDirectory(@Nonnull VirtualFile file, @Nonnull Project project);

  void persistAttribute(@Nonnull Project project, @Nonnull VirtualFile fileOrDir, @Nonnull T value) throws IOException;

  /**
   * @deprecated not used anymore
   */
  @Deprecated
  //@ApiStatus.ScheduledForRemoval(inVersion = "2021.3")
  interface Engine {
    void pushAll();

    void pushRecursively(@Nonnull VirtualFile vile, @Nonnull Project project);
  }

  void afterRootsChanged(@Nonnull Project project);
}
