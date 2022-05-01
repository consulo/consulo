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
package consulo.roots.ui.configuration.impl;

import consulo.project.Project;
import consulo.content.OrderRootType;
import consulo.module.impl.internal.layer.library.LibraryTableImplUtil;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.ide.setting.module.event.LibraryEditorListener;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.StructureLibraryTableModifiableModelProvider;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.annotation.access.RequiredWriteAction;
import consulo.disposer.Disposable;
import consulo.ide.setting.module.LibrariesConfigurator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19/04/2021
 */
public class DefaultLibrariesConfigurator implements LibrariesConfigurator {
  public final Map<String, LibrariesModifiableModel> myLevel2Providers = new HashMap<>();
  private final EventDispatcher<LibraryEditorListener> myLibraryEditorListeners = EventDispatcher.create(LibraryEditorListener.class);

  private final Project myProject;

  public DefaultLibrariesConfigurator(Project project) {
    myProject = project;
  }

  @RequiredWriteAction
  @Override
  public void commit() {
    for (final LibrariesModifiableModel provider : myLevel2Providers.values()) {
      provider.deferredCommit();
    }
  }

  @Override
  public void dispose() {
    for (final LibrariesModifiableModel provider : myLevel2Providers.values()) {
      provider.disposeUncommittedLibraries();
    }
  }

  @Override
  public void reset() {
    final LibraryTablesRegistrar tablesRegistrar = LibraryTablesRegistrar.getInstance();

    myLevel2Providers.clear();
    myLevel2Providers.put(LibraryTablesRegistrar.PROJECT_LEVEL, new LibrariesModifiableModel(tablesRegistrar.getLibraryTable(myProject), myProject, this));
  }

  @Override
  public LibraryTableModifiableModelProvider createModifiableModelProvider(final String level) {
    return new StructureLibraryTableModifiableModelProvider(level, this);
  }

  @Override
  public LibraryTableModifiableModelProvider getProjectLibrariesProvider() {
    return createModifiableModelProvider(LibraryTablesRegistrar.PROJECT_LEVEL);
  }

  @Nullable
  public LibrariesModifiableModel getLibrariesModifiableModel(String level) {
    return myLevel2Providers.get(level);
  }

  @Override
  public void libraryCreated(@Nonnull Library library) {
    myLibraryEditorListeners.getMulticaster().libraryCreated(library);
  }

  @Override
  public void libraryRenamed(@Nonnull Library library, String oldName, String newName) {
    myLibraryEditorListeners.getMulticaster().libraryRenamed(library, oldName, newName);
  }

  @Nonnull
  @Override
  public VirtualFile[] getLibraryFiles(Library library, OrderRootType type) {
    final LibraryTable table = library.getTable();
    if (table != null) {
      final LibraryTable.ModifiableModel modifiableModel = getModifiableLibraryTable(table);
      if (modifiableModel instanceof LibrariesModifiableModel) {
        final LibrariesModifiableModel librariesModel = (LibrariesModifiableModel)modifiableModel;
        if (librariesModel.hasLibraryEditor(library)) {
          return librariesModel.getLibraryEditor(library).getFiles(type);
        }
      }
    }
    return library.getFiles(type);
  }

  @Nonnull
  @Override
  public LibraryTable.ModifiableModel getModifiableLibraryTable(@Nonnull LibraryTable table) {
    final String tableLevel = table.getTableLevel();
    if (tableLevel.equals(LibraryTableImplUtil.MODULE_LEVEL)) {
      return table.getModifiableModel();
    }
    return myLevel2Providers.get(tableLevel);
  }

  @Override
  @Nullable
  public Library getLibrary(final String libraryName, final String libraryLevel) {
    /* the null check is added only to prevent NPE when called from getLibrary */
    final LibrariesModifiableModel model = myLevel2Providers.get(libraryLevel);
    return model == null ? null : findLibraryModel(libraryName, model);
  }

  @Override
  public void addLibraryEditorListener(@Nonnull LibraryEditorListener listener) {
    myLibraryEditorListeners.addListener(listener);
  }

  @Override
  public void addLibraryEditorListener(@Nonnull LibraryEditorListener listener, @Nonnull Disposable parentDisposable) {
    myLibraryEditorListeners.addListener(listener, parentDisposable);
  }

  @Override
  public Collection<? extends LibraryTable.ModifiableModel> getModels() {
    return myLevel2Providers.values();
  }

  @Override
  @Nullable
  public Library getLibraryModel(@Nonnull Library library) {
    final LibraryTable libraryTable = library.getTable();
    if (libraryTable != null) {
      return findLibraryModel(library, myLevel2Providers.get(libraryTable.getTableLevel()));
    }
    return library;
  }

  @Nullable
  private static Library findLibraryModel(final @Nonnull String libraryName, @Nonnull LibrariesModifiableModel model) {
    for (Library library : model.getLibraries()) {
      final Library libraryModel = findLibraryModel(library, model);
      if (libraryModel != null && libraryName.equals(libraryModel.getName())) {
        return libraryModel;
      }
    }
    return null;
  }

  @Nullable
  private static Library findLibraryModel(final Library library, LibrariesModifiableModel tableModel) {
    if (tableModel == null) return library;
    if (tableModel.wasLibraryRemoved(library)) return null;
    return tableModel.hasLibraryEditor(library) ? (Library)tableModel.getLibraryEditor(library).getModel() : library;
  }
}
