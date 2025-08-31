/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.project.Project;
import consulo.application.content.impl.internal.library.LibraryImpl;
import consulo.application.content.impl.internal.library.LibraryTableBase;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.PersistentLibraryKind;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.ExistingLibraryEditor;
import consulo.content.library.ui.LibraryEditor;
import consulo.ide.setting.module.event.LibraryEditorListener;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.disposer.Disposer;
import consulo.util.collection.Maps;
import consulo.util.collection.Sets;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author anna
 * @since 2006-06-04
 */
public class LibrariesModifiableModel implements LibraryTableBase.ModifiableModelEx {
  //todo[nik] remove LibraryImpl#equals method instead of using identity maps
  private final Map<Library, ExistingLibraryEditor> myLibrary2EditorMap = Maps.newHashMap(ContainerUtil.identityStrategy());
  private final Set<Library> myRemovedLibraries = Sets.newHashSet(ContainerUtil.identityStrategy());

  private LibraryTable.ModifiableModel myLibrariesModifiableModel;
  private final Project myProject;
  private final LibraryTable myTable;
  private final LibraryEditorListener myLibraryEditorListener;

  public LibrariesModifiableModel(LibraryTable table, Project project, LibraryEditorListener libraryEditorListener) {
    myProject = project;
    myTable = table;
    myLibraryEditorListener = libraryEditorListener;
  }

  @Override
  public Library createLibrary(String name, @Nullable PersistentLibraryKind type) {
    Library library = getLibrariesModifiableModel().createLibrary(name, type);
    //createLibraryEditor(library);
    myLibraryEditorListener.libraryCreated(library);
    return library;
  }

  @Override
  public void removeLibrary(@Nonnull Library library) {
    if (getLibrariesModifiableModel().getLibraryByName(library.getName()) == null) return;

    removeLibraryEditor(library);
    Library existingLibrary = myTable.getLibraryByName(library.getName());
    getLibrariesModifiableModel().removeLibrary(library);
    if (existingLibrary == library) {
      myRemovedLibraries.add(library);
    }
    else {
      // dispose uncommitted library
      Disposer.dispose(library);
    }
  }

  @Override
  public void commit() {
    //do nothing  - do deffered commit
  }

  @Override
  @Nonnull
  public Iterator<Library> getLibraryIterator() {
    return getLibrariesModifiableModel().getLibraryIterator();
  }

  @Override
  public Library getLibraryByName(@Nonnull String name) {
    return getLibrariesModifiableModel().getLibraryByName(name);
  }

  @Override
  @Nonnull
  public Library[] getLibraries() {
    return getLibrariesModifiableModel().getLibraries();
  }

  @Override
  public boolean isChanged() {
    for (LibraryEditor libraryEditor : myLibrary2EditorMap.values()) {
      if (libraryEditor.hasChanges()) return true;
    }
    return getLibrariesModifiableModel().isChanged();
  }

  public void deferredCommit() {
    List<ExistingLibraryEditor> libraryEditors = new ArrayList<ExistingLibraryEditor>(myLibrary2EditorMap.values());
    myLibrary2EditorMap.clear();
    for (ExistingLibraryEditor libraryEditor : libraryEditors) {
      libraryEditor.commit(); // TODO: is seems like commit will recreate the editor, but it should not
      Disposer.dispose(libraryEditor);
    }
    if (!libraryEditors.isEmpty() || !myRemovedLibraries.isEmpty() || myLibrariesModifiableModel != null && myLibrariesModifiableModel.isChanged()) {
      getLibrariesModifiableModel().commit();
      myLibrariesModifiableModel = null;
    }
    myRemovedLibraries.clear();
  }

  public boolean wasLibraryRemoved(Library library) {
    return myRemovedLibraries.contains(library);
  }

  public boolean hasLibraryEditor(Library library) {
    return myLibrary2EditorMap.containsKey(library);
  }

  public ExistingLibraryEditor getLibraryEditor(Library library) {
    Library source = ((LibraryImpl)library).getSource();
    if (source != null) {
      return getLibraryEditor(source);
    }
    ExistingLibraryEditor libraryEditor = myLibrary2EditorMap.get(library);
    if (libraryEditor == null) {
      libraryEditor = createLibraryEditor(library);
    }
    return libraryEditor;
  }

  private ExistingLibraryEditor createLibraryEditor(Library library) {
    ExistingLibraryEditor libraryEditor = new ExistingLibraryEditor(library, myLibraryEditorListener);
    myLibrary2EditorMap.put(library, libraryEditor);
    return libraryEditor;
  }

  private void removeLibraryEditor(Library library) {
    ExistingLibraryEditor libraryEditor = myLibrary2EditorMap.remove(library);
    if (libraryEditor != null) {
      Disposer.dispose(libraryEditor);
    }
  }

  public Library.ModifiableModel getLibraryModifiableModel(Library library) {
    return getLibraryEditor(library).getModel();
  }

  private LibraryTable.ModifiableModel getLibrariesModifiableModel() {
    if (myLibrariesModifiableModel == null) {
      myLibrariesModifiableModel = myTable.getModifiableModel();
    }

    return myLibrariesModifiableModel;
  }

  public void disposeUncommittedLibraries() {
    for (Library library : new ArrayList<Library>(myLibrary2EditorMap.keySet())) {
      Library existingLibrary = myTable.getLibraryByName(library.getName());
      if (existingLibrary != library) {
        Disposer.dispose(library);
      }

      ExistingLibraryEditor libraryEditor = myLibrary2EditorMap.get(library);
      if (libraryEditor != null) {
        Disposer.dispose(libraryEditor);
      }
    }

    myLibrary2EditorMap.clear();
  }
}
