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
package com.intellij.openapi.roots.libraries;

import com.intellij.openapi.project.Project;
import consulo.disposer.Disposable;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.EventListener;
import java.util.Iterator;

/**
 * @see LibraryTablesRegistrar#getLibraryTable(Project)
 * @author dsl
 */
public interface LibraryTable {
  @Nonnull
  Library[] getLibraries();

  Library createLibrary();

  Library createLibrary(@NonNls String name);

  void removeLibrary(@Nonnull Library library);

  @Nonnull
  Iterator<Library> getLibraryIterator();

  @Nullable
  Library getLibraryByName(@Nonnull String name);

  String getTableLevel();

  LibraryTablePresentation getPresentation();

  boolean isEditable();

  ModifiableModel getModifiableModel();

  void addListener(Listener listener);
  
  void addListener(Listener listener, Disposable parentDisposable);

  void removeListener(Listener listener);

  interface ModifiableModel {
    Library createLibrary(String name);
    
    void removeLibrary(@Nonnull Library library);

    void commit();

    @Nonnull
    Iterator<Library> getLibraryIterator();

    @Nullable
    Library getLibraryByName(@Nonnull String name);

    @Nonnull
    Library[] getLibraries();

    boolean isChanged();
  }

  interface Listener extends EventListener{
    void afterLibraryAdded (Library newLibrary);
    void afterLibraryRenamed (Library library);
    void beforeLibraryRemoved (Library library);
    void afterLibraryRemoved (Library library);
  }
}
