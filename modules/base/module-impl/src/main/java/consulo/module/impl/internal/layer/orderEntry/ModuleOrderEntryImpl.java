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

package consulo.module.impl.internal.layer.orderEntry;

import consulo.component.util.pointer.NamedPointer;
import consulo.content.OrderRootType;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.layer.OrderRootsEnumerator;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.impl.internal.ModuleRootManagerImpl;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author dsl
 */
public class ModuleOrderEntryImpl extends OrderEntryBaseImpl implements ModuleOrderEntry, ClonableOrderEntry {
  private final NamedPointer<Module> myModulePointer;
  private boolean myExported;
  @Nonnull
  private DependencyScope myScope = DependencyScope.COMPILE;
  private boolean myProductionOnTestDependency;

  public ModuleOrderEntryImpl(@Nonnull Module module, @Nonnull ModuleRootLayerImpl rootLayer) {
    super(ModuleOrderEntryType.getInstance(), rootLayer);
    myModulePointer = ModuleUtilCore.createPointer(module);
  }

  public ModuleOrderEntryImpl(@Nonnull String moduleName, @Nonnull ModuleRootLayerImpl rootLayer) {
    this(moduleName, rootLayer, DependencyScope.COMPILE, false, false);
  }

  public ModuleOrderEntryImpl(@Nonnull String moduleName, @Nonnull ModuleRootLayerImpl rootLayer, @Nonnull DependencyScope dependencyScope, boolean exported,
                              boolean productionOnTestDependency) {
    super(ModuleOrderEntryType.getInstance(), rootLayer);
    myModulePointer = ModuleUtilCore.createPointer(rootLayer.getProject(), moduleName);
    myScope = dependencyScope;
    myExported = exported;
    myProductionOnTestDependency = productionOnTestDependency;
  }

  private ModuleOrderEntryImpl(ModuleOrderEntryImpl that, ModuleRootLayerImpl rootLayer) {
    super(ModuleOrderEntryType.getInstance(), rootLayer);
    NamedPointer<Module> thatModule = that.myModulePointer;
    myModulePointer = ModuleUtilCore.createPointer(rootLayer.getProject(), thatModule.getName());
    myExported = that.myExported;
    myProductionOnTestDependency = that.myProductionOnTestDependency;
    myScope = that.myScope;
  }

  @Override
  @Nonnull
  public Module getOwnerModule() {
    return getRootModel().getModule();
  }

  @Override
  public boolean isProductionOnTestDependency() {
    return myProductionOnTestDependency;
  }

  @Override
  public void setProductionOnTestDependency(boolean productionOnTestDependency) {
    myProductionOnTestDependency = productionOnTestDependency;
  }

  @Override
  @Nonnull
  public VirtualFile[] getFiles(OrderRootType type) {
    OrderRootsEnumerator enumerator = getEnumerator(type);
    return enumerator != null ? enumerator.getRoots() : VirtualFile.EMPTY_ARRAY;
  }

  @Nullable
  private OrderRootsEnumerator getEnumerator(OrderRootType type) {
    Module module = myModulePointer.get();
    if (module == null) return null;

    return ModuleRootManagerImpl.getCachingEnumeratorForType(type, module);
  }

  @Override
  @Nonnull
  public String[] getUrls(OrderRootType rootType) {
    OrderRootsEnumerator enumerator = getEnumerator(rootType);
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
  public boolean isEquivalentTo(@Nonnull OrderEntry other) {
    return other instanceof ModuleOrderEntry && Comparing.equal(getModuleName(), ((ModuleOrderEntry)other).getModuleName());
  }

  @Override
  @Nonnull
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
  @Nonnull
  public DependencyScope getScope() {
    return myScope;
  }

  @Override
  public void setScope(@Nonnull DependencyScope scope) {
    getRootModel().assertWritable();
    myScope = scope;
  }
}
