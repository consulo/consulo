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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.roots.orderEntry.ModuleOrderEntryType;

/**
 * @author dsl
 */
public class ModuleOrderEntryImpl extends OrderEntryBaseImpl implements ModuleOrderEntry, ClonableOrderEntry {
  private final NamedPointer<Module> myModulePointer;
  private boolean myExported;
  @NotNull
  private DependencyScope myScope = DependencyScope.COMPILE;
  private boolean myProductionOnTestDependency;

  ModuleOrderEntryImpl(@NotNull Module module, @NotNull ModuleRootLayerImpl rootLayer) {
    super(ModuleOrderEntryType.getInstance(), rootLayer);
    myModulePointer = ModuleUtilCore.createPointer(module);
  }

  ModuleOrderEntryImpl(@NotNull String moduleName, @NotNull ModuleRootLayerImpl rootLayer) {
    this(moduleName, rootLayer, DependencyScope.COMPILE, false, false);
  }

  public ModuleOrderEntryImpl(@NotNull String moduleName, @NotNull ModuleRootLayerImpl rootLayer, @NotNull DependencyScope dependencyScope, boolean exported,
                       boolean productionOnTestDependency) {
    super(ModuleOrderEntryType.getInstance(), rootLayer);
    myModulePointer = ModuleUtilCore.createPointer(rootLayer.getProject(), moduleName);
    myScope = dependencyScope;
    myExported = exported;
    myProductionOnTestDependency = productionOnTestDependency;
  }

  private ModuleOrderEntryImpl(ModuleOrderEntryImpl that, ModuleRootLayerImpl rootLayer) {
    super(ModuleOrderEntryType.getInstance(), rootLayer);
    final NamedPointer<Module> thatModule = that.myModulePointer;
    myModulePointer = ModuleUtilCore.createPointer(rootLayer.getProject(), thatModule.getName());
    myExported = that.myExported;
    myProductionOnTestDependency = that.myProductionOnTestDependency;
    myScope = that.myScope;
  }

  @Override
  @NotNull
  public Module getOwnerModule() {
    return getRootModel().getModule();
  }

  public boolean isProductionOnTestDependency() {
    return myProductionOnTestDependency;
  }

  public void setProductionOnTestDependency(boolean productionOnTestDependency) {
    myProductionOnTestDependency = productionOnTestDependency;
  }

  @Override
  @NotNull
  public VirtualFile[] getFiles(OrderRootType type) {
    final OrderRootsEnumerator enumerator = getEnumerator(type);
    return enumerator != null ? enumerator.getRoots() : VirtualFile.EMPTY_ARRAY;
  }

  @Nullable
  private OrderRootsEnumerator getEnumerator(OrderRootType type) {
    final Module module = myModulePointer.get();
    if (module == null) return null;

    return ModuleRootManagerImpl.getCachingEnumeratorForType(type, module);
  }

  @Override
  @NotNull
  public String[] getUrls(OrderRootType rootType) {
    final OrderRootsEnumerator enumerator = getEnumerator(rootType);
    return enumerator != null ? enumerator.getUrls() : ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public boolean isValid() {
    return !isDisposed() && getModule() != null;
  }

  @Override
  public <R> R accept(RootPolicy<R> policy, R initialValue) {
    return policy.visitModuleOrderEntry(this, initialValue);
  }

  @Override
  public boolean isEquivalentTo(@NotNull OrderEntry other) {
    return other instanceof ModuleOrderEntry && Comparing.equal(getModuleName(), ((ModuleOrderEntry)other).getModuleName());
  }

  @Override
  @NotNull
  public String getPresentableName() {
    return getModuleName();
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  @Nullable
  public Module getModule() {
    return getRootModel().getConfigurationAccessor().getModule(myModulePointer.get(), myModulePointer.getName());
  }

  @Override
  public String getModuleName() {
    return myModulePointer.getName();
  }

  @Override
  public OrderEntry cloneEntry(ModuleRootLayerImpl rootModel) {
    return new ModuleOrderEntryImpl(this, rootModel);
  }

  @Override
  public boolean isExported() {
    return myExported;
  }

  @Override
  public void setExported(boolean value) {
    getRootModel().assertWritable();
    myExported = value;
  }

  @Override
  @NotNull
  public DependencyScope getScope() {
    return myScope;
  }

  @Override
  public void setScope(@NotNull DependencyScope scope) {
    getRootModel().assertWritable();
    myScope = scope;
  }
}
