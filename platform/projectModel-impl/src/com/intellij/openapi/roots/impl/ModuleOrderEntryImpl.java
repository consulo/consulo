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
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.consulo.util.pointers.NamedPointer;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dsl
 */
public class ModuleOrderEntryImpl extends OrderEntryBaseImpl implements ModuleOrderEntry, WritableOrderEntry, ClonableOrderEntry {
  @NonNls public static final String ENTRY_TYPE = "module";
  @NonNls public static final String MODULE_NAME_ATTR = "module-name";
  @NonNls private static final String EXPORTED_ATTR = "exported";
  @NonNls private static final String PRODUCTION_ON_TEST_ATTRIBUTE = "production-on-test";

  private final NamedPointer<Module> myModulePointer;
  private boolean myExported = false;
  @NotNull private DependencyScope myScope;
  private boolean myProductionOnTestDependency;

  ModuleOrderEntryImpl(@NotNull Module module, @NotNull ModuleRootLayerImpl rootLayer) {
    super(rootLayer);
    myModulePointer = ModuleUtilCore.createPointer(module);
    myScope = DependencyScope.COMPILE;
  }

  ModuleOrderEntryImpl(@NotNull String moduleName, @NotNull ModuleRootLayerImpl rootLayer) {
    super(rootLayer);
    myModulePointer = ModuleUtilCore.createPointer(rootLayer.getProject(), moduleName);
    myScope = DependencyScope.COMPILE;
  }

  ModuleOrderEntryImpl(Element element, ModuleRootLayerImpl rootLayer) throws InvalidDataException {
    super(rootLayer);
    myExported = element.getAttributeValue(EXPORTED_ATTR) != null;
    final String moduleName = element.getAttributeValue(MODULE_NAME_ATTR);
    if (moduleName == null) {
      throw new InvalidDataException();
    }

    myModulePointer = ModuleUtilCore.createPointer(rootLayer.getProject(), moduleName);
    myScope = DependencyScope.readExternal(element);
    myProductionOnTestDependency = element.getAttributeValue(PRODUCTION_ON_TEST_ATTRIBUTE) != null;
  }

  private ModuleOrderEntryImpl(ModuleOrderEntryImpl that, ModuleRootLayerImpl rootLayer) {
    super(rootLayer);
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
  public void writeExternal(Element rootElement) throws WriteExternalException {
    final Element element = OrderEntryFactory.createOrderEntryElement(ENTRY_TYPE);
    element.setAttribute(MODULE_NAME_ATTR, getModuleName());
    if (myExported) {
      element.setAttribute(EXPORTED_ATTR, "");
    }
    myScope.writeExternal(element);
    if (myProductionOnTestDependency) {
      element.setAttribute(PRODUCTION_ON_TEST_ATTRIBUTE, "");
    }
    rootElement.addContent(element);
  }

  @Override
  public String getModuleName() {
    return myModulePointer.getName();
  }

  @Override
  public OrderEntry cloneEntry(ModuleRootLayerImpl rootModel, ProjectRootManagerImpl projectRootManager) {
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
