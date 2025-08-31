/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.setting.module;

import consulo.annotation.access.RequiredWriteAction;
import consulo.content.OrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.disposer.Disposable;
import consulo.ide.setting.module.event.LibraryEditorListener;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 19/04/2021
 */
public interface LibrariesConfigurator extends LibraryEditorListener, Disposable {
  @Nonnull
  VirtualFile[] getLibraryFiles(Library library, OrderRootType type);

  @Nonnull
  LibraryTable.ModifiableModel getModifiableLibraryTable(@Nonnull LibraryTable table);

  @Nullable
  Library getLibrary(String libraryName, String libraryLevel);

  @Nonnull
  LibraryTableModifiableModelProvider createModifiableModelProvider(String level);

  @Nonnull
  LibraryTableModifiableModelProvider getProjectLibrariesProvider();

  @Nullable
  Library getLibraryModel(@Nonnull Library library);

  Collection<? extends LibraryTable.ModifiableModel> getModels();

  void addLibraryEditorListener(@Nonnull LibraryEditorListener listener);

  void addLibraryEditorListener(@Nonnull LibraryEditorListener listener, @Nonnull Disposable parentDisposable);

  @RequiredWriteAction
  void commit();

  void reset();
}
