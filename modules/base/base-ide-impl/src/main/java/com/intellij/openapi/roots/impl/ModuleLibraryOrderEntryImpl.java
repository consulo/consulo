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

package com.intellij.openapi.roots.impl;

import consulo.logging.Logger;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import consulo.disposer.Disposer;
import com.intellij.util.PathUtil;
import consulo.roots.impl.ModuleRootLayerImpl;
import consulo.roots.orderEntry.ModuleLibraryOrderEntryType;
import consulo.roots.types.BinariesOrderRootType;
import javax.annotation.Nonnull;

/**
 * Library entry for module ("in-place") libraries
 *
 * @author dsl
 */
public class ModuleLibraryOrderEntryImpl extends LibraryOrderEntryBaseImpl implements LibraryOrderEntry, ClonableOrderEntry {
  public static final Logger LOGGER = Logger.getInstance(ModuleLibraryOrderEntryImpl.class);

  private final Library myLibrary;
  private boolean myExported;

  public ModuleLibraryOrderEntryImpl(Library library, ModuleRootLayerImpl rootLayer, boolean isExported, DependencyScope scope, boolean init) {
    super(ModuleLibraryOrderEntryType.getInstance(), rootLayer, ProjectRootManagerImpl.getInstanceImpl(rootLayer.getProject()));
    myLibrary = library;
    myExported = isExported;
    myScope = scope;
    Disposer.register(this, myLibrary);
    if (init) {
      init();
    }
  }

  ModuleLibraryOrderEntryImpl(String name, final PersistentLibraryKind kind, ModuleRootLayerImpl moduleRootLayer) {
    super(ModuleLibraryOrderEntryType.getInstance(), moduleRootLayer, ProjectRootManagerImpl.getInstanceImpl(moduleRootLayer.getProject()));
    myLibrary = LibraryTableImplUtil.createModuleLevelLibrary(name, kind, moduleRootLayer);
    Disposer.register(this, myLibrary);
    init();
  }

  @Override
  protected RootProvider getRootProvider() {
    return myLibrary.getRootProvider();
  }

  @Override
  public Library getLibrary() {
    return myLibrary;
  }

  @Override
  public boolean isModuleLevel() {
    return true;
  }

  @Override
  public String getLibraryName() {
    return myLibrary.getName();
  }

  @Override
  public String getLibraryLevel() {
    return LibraryTableImplUtil.MODULE_LEVEL;
  }

  @Nonnull
  @Override
  public String getPresentableName() {
    final String name = myLibrary.getName();
    if (name != null) {
      return name;
    }
    else {
      if (myLibrary instanceof LibraryEx && ((LibraryEx)myLibrary).isDisposed()) {
        return "<unknown>";
      }

      final String[] urls = myLibrary.getUrls(BinariesOrderRootType.getInstance());
      if (urls.length > 0) {
        String url = urls[0];
        return PathUtil.toPresentableUrl(url);
      }
      else {
        return ProjectBundle.message("library.empty.library.item");
      }
    }
  }

  @Override
  public boolean isValid() {
    return !isDisposed() && myLibrary != null;
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, R initialValue) {
    return policy.visitLibraryOrderEntry(this, initialValue);
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public OrderEntry cloneEntry(ModuleRootLayerImpl rootModel) {
    return new ModuleLibraryOrderEntryImpl(((LibraryImpl)myLibrary).cloneLibrary(rootModel), rootModel, myExported, myScope, true);
  }

  @Override
  public boolean isExported() {
    return myExported;
  }

  @Override
  public void setExported(boolean value) {
    myExported = value;
  }

  @Override
  @Nonnull
  public DependencyScope getScope() {
    return myScope;
  }

  @Override
  public void setScope(@Nonnull DependencyScope scope) {
    myScope = scope;
  }
}
