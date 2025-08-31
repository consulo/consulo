/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.setting;

import consulo.module.Module;
import consulo.content.bundle.Sdk;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.content.library.Library;
import consulo.compiler.artifact.Artifact;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-03-14
 */
public interface ProjectStructureSelector {
  Key<ProjectStructureSelector> KEY = Key.create("project.structure.editor");

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> select(@Nullable Artifact artifact, boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> select(@Nonnull Sdk sdk, boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  default AsyncResult<Void> select(@Nonnull Module module, boolean requestFocus) {
    return select(module.getName(), null, requestFocus);
  }

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> select(@Nullable String moduleToSelect, @Nullable String tabId, boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> select(@Nonnull LibraryOrderEntry libraryOrderEntry, boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> selectOrderEntry(@Nonnull Module module, @Nullable OrderEntry orderEntry);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> selectProjectOrGlobalLibrary(@Nonnull Library library, boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> selectProjectGeneralSettings(boolean requestFocus);
}
