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
package consulo.options;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.packaging.artifacts.Artifact;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  AsyncResult<Void> select(@Nonnull Sdk sdk, final boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> select(@Nullable final String moduleToSelect, @Nullable String tabId, final boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> select(@Nonnull LibraryOrderEntry libraryOrderEntry, final boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> selectOrderEntry(@Nonnull final Module module, @Nullable final OrderEntry orderEntry);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> selectProjectOrGlobalLibrary(@Nonnull Library library, boolean requestFocus);

  @Nonnull
  @RequiredUIAccess
  AsyncResult<Void> selectProjectGeneralSettings(final boolean requestFocus);
}
