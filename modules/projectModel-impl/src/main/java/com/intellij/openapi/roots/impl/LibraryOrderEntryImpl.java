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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Comparing;
import consulo.roots.impl.ModuleRootLayerImpl;
import consulo.roots.orderEntry.LibraryOrderEntryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dsl
 */
public class LibraryOrderEntryImpl extends LibraryOrderEntryBaseImpl implements LibraryOrderEntry, ClonableOrderEntry {
  public static final Logger LOGGER = Logger.getInstance(LibraryOrderEntryImpl.class);

  private Library myLibrary;
  @Nullable
  private String myLibraryName; // is non-null if myLibrary == null
  @Nullable
  private String myLibraryLevel; // is non-null if myLibraryLevel == null

  private boolean myExported;

  private final MyOrderEntryLibraryTableListener myLibraryListener = new MyOrderEntryLibraryTableListener();

  public LibraryOrderEntryImpl(@NotNull Library library, @NotNull ModuleRootLayerImpl rootLayer) {
    super(LibraryOrderEntryType.getInstance(), rootLayer, ProjectRootManagerImpl.getInstanceImpl(rootLayer.getProject()));
    LOGGER.assertTrue(library.getTable() != null);
    myLibrary = library;
    addListeners();
    init();
  }

  private LibraryOrderEntryImpl(@NotNull LibraryOrderEntryImpl that, @NotNull ModuleRootLayerImpl rootLayer) {
    super(LibraryOrderEntryType.getInstance(), rootLayer, ProjectRootManagerImpl.getInstanceImpl(rootLayer.getProject()));
    if (that.myLibrary == null) {
      myLibraryName = that.myLibraryName;
      myLibraryLevel = that.myLibraryLevel;
    }
    else {
      myLibrary = that.myLibrary;
    }
    myExported = that.myExported;
    myScope = that.myScope;
    addListeners();
    init();
  }

  public LibraryOrderEntryImpl(@NotNull String name, @NotNull String level, @NotNull ModuleRootLayerImpl rootLayer) {
    this(name, level, rootLayer, DependencyScope.COMPILE, false, true);
  }

  public LibraryOrderEntryImpl(@NotNull String name,
                               @NotNull String level,
                               @NotNull ModuleRootLayerImpl rootLayer,
                               DependencyScope dependencyScope,
                               boolean exported,
                               boolean init) {
    super(LibraryOrderEntryType.getInstance(), rootLayer, ProjectRootManagerImpl.getInstanceImpl(rootLayer.getProject()));
    myScope = dependencyScope;
    myExported = exported;
    searchForLibrary(name, level);
    addListeners();
    if(init) {
      init();
    }
  }

  private void searchForLibrary(@NotNull String name, @NotNull String level) {
    if (myLibrary != null) return;
    final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(level, getRootModel().getModule().getProject());
    final Library library = libraryTable != null ? libraryTable.getLibraryByName(name) : null;
    if (library == null) {
      myLibraryName = name;
      myLibraryLevel = level;
      myLibrary = null;
    }
    else {
      myLibraryName = null;
      myLibraryLevel = null;
      myLibrary = library;
    }
  }

  @Override
  public boolean isExported() {
    return myExported;
  }

  @Override
  public void setExported(boolean exported) {
    myExported = exported;
  }

  @Override
  @NotNull
  public DependencyScope getScope() {
    return myScope;
  }

  @Override
  public void setScope(@NotNull DependencyScope scope) {
    myScope = scope;
  }

  @Override
  @Nullable
  public Library getLibrary() {
    Library library = getRootModel().getConfigurationAccessor().getLibrary(myLibrary, myLibraryName, myLibraryLevel);
    if (library != null) { //library was not deleted
      return library;
    }
    if (myLibrary != null) {
      myLibraryName = myLibrary.getName();
      myLibraryLevel = myLibrary.getTable().getTableLevel();
    }
    myLibrary = null;
    return null;
  }

  @Override
  public boolean isModuleLevel() {
    return false;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return getLibraryName();
  }

  @Override
  @Nullable
  protected RootProvider getRootProvider() {
    return myLibrary == null ? null : myLibrary.getRootProvider();
  }

  @Override
  public boolean isValid() {
    if (isDisposed()) {
      return false;
    }
    Library library = getLibrary();
    return library != null && !((LibraryEx)library).isDisposed();
  }

  @Override
  public <R> R accept(@NotNull RootPolicy<R> policy, R initialValue) {
    return policy.visitLibraryOrderEntry(this, initialValue);
  }

  @Override
  @NotNull
  public OrderEntry cloneEntry(@NotNull ModuleRootLayerImpl moduleRootLayer) {
    return new LibraryOrderEntryImpl(this, moduleRootLayer);
  }

  @Override
  @Nullable
  public String getLibraryLevel() {
    if (myLibrary != null) {
      final LibraryTable table = myLibrary.getTable();
      return table.getTableLevel();
    }
    else {
      return myLibraryLevel;
    }
  }

  @Override
  public String getLibraryName() {
    return myLibrary == null ? myLibraryName : myLibrary.getName();
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public void dispose() {
    super.dispose();
    final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(getLibraryLevel(), getRootModel().getProject());
    if (libraryTable != null) {
      myProjectRootManagerImpl.removeListenerForTable(myLibraryListener, libraryTable);
    }
  }

  private void addListeners() {
    final String libraryLevel = getLibraryLevel();
    final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(libraryLevel, getRootModel().getProject());
    if (libraryTable != null) {
      myProjectRootManagerImpl.addListenerForTable(myLibraryListener, libraryTable);
    }
  }

  private void afterLibraryAdded(@NotNull Library newLibrary) {
    if (myLibrary == null) {
      if (Comparing.equal(myLibraryName, newLibrary.getName())) {
        myLibrary = newLibrary;
        myLibraryName = null;
        myLibraryLevel = null;
        updateFromRootProviderAndSubscribe();
      }
    }
  }

  private void beforeLibraryRemoved(Library library) {
    if (library == myLibrary) {
      myLibraryName = myLibrary.getName();
      myLibraryLevel = myLibrary.getTable().getTableLevel();
      myLibrary = null;
      updateFromRootProviderAndSubscribe();
    }
  }

  private class MyOrderEntryLibraryTableListener implements LibraryTable.Listener {
    public MyOrderEntryLibraryTableListener() {
    }

    @Override
    public void afterLibraryAdded(@NotNull Library newLibrary) {
      LibraryOrderEntryImpl.this.afterLibraryAdded(newLibrary);
    }

    @Override
    public void afterLibraryRenamed(@NotNull Library library) {
      afterLibraryAdded(library);
    }

    @Override
    public void beforeLibraryRemoved(Library library) {
      LibraryOrderEntryImpl.this.beforeLibraryRemoved(library);
    }

    @Override
    public void afterLibraryRemoved(Library library) {
    }
  }
}
