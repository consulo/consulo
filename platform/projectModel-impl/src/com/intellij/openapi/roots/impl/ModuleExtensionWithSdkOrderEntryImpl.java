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

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.consulo.module.extension.ModuleExtensionProviderEP;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.module.JpsModuleRootModelSerializer;

/**
 * @author dsl
 */
public class ModuleExtensionWithSdkOrderEntryImpl extends LibraryOrderEntryBaseImpl
  implements WritableOrderEntry, ClonableOrderEntry, ModuleExtensionWithSdkOrderEntry {
  @NonNls public static final String ENTRY_TYPE = JpsModuleRootModelSerializer.MODULE_EXTENSION_SDK;

  @NonNls public static final String EXTENSION_ID_ATTRIBUTE = JpsModuleRootModelSerializer.EXTENSION_ID_ATTRIBUTE;

  private String myModuleExtensionId;
  @Nullable
  private ModuleExtensionWithSdk<?> myModuleExtension;

  ModuleExtensionWithSdkOrderEntryImpl(@NotNull ModuleExtensionWithSdk<?> moduleExtension,
                                       @NotNull RootModelImpl rootModel,
                                       @NotNull ProjectRootManagerImpl projectRootManager) {
    super(rootModel, projectRootManager);
    init(moduleExtension, moduleExtension.getId());
    myModuleExtension = moduleExtension;
  }

  ModuleExtensionWithSdkOrderEntryImpl(@NotNull Element element,
                                       @NotNull RootModelImpl rootModel,
                                       @NotNull ProjectRootManagerImpl projectRootManager) throws InvalidDataException {
    super(rootModel, projectRootManager);
    if (!element.getName().equals(OrderEntryFactory.ORDER_ENTRY_ELEMENT_NAME)) {
      throw new InvalidDataException();
    }

    final String moduleExtensionId = element.getAttributeValue(EXTENSION_ID_ATTRIBUTE);
    final ModuleExtensionProvider provider = ModuleExtensionProviderEP.findProvider(moduleExtensionId);
    final ModuleExtensionWithSdk extension = provider == null ? null : (ModuleExtensionWithSdk) rootModel.getExtensionWithoutCheck(provider.getImmutableClass());

    init(extension, moduleExtensionId);
  }

  private ModuleExtensionWithSdkOrderEntryImpl(@NotNull ModuleExtensionWithSdkOrderEntryImpl that,
                                               @NotNull RootModelImpl rootModel,
                                               @NotNull ProjectRootManagerImpl projectRootManager) {
    super(rootModel, projectRootManager);
    init(that.getModuleExtension(), that.getModuleExtensionId());
  }


  private void init(final ModuleExtensionWithSdk moduleExtension, String extensionId) {
    myModuleExtension = moduleExtension;
    myModuleExtensionId = extensionId;
    init();
  }

  @Override
  protected RootProvider getRootProvider() {
    final ModuleExtensionWithSdk<?> moduleExtension = getModuleExtension();
    if(moduleExtension == null) {
      return null;
    }
    final Sdk sdk = moduleExtension.getSdk();
    if(sdk == null) {
      return null;
    }
    return sdk.getRootProvider();
  }

  @Override
  @Nullable
  public Sdk getSdk() {
    final ModuleExtensionWithSdk<?> moduleExtension = getModuleExtension();
    if(moduleExtension == null) {
      return null;
    }
    return moduleExtension.getSdk();
  }

  @Override
  @Nullable
  public String getSdkName() {
    final ModuleExtensionWithSdk<?> moduleExtension = getModuleExtension();
    if(moduleExtension == null) {
      return null;
    }
    final Sdk sdk = moduleExtension.getSdk();
    if(sdk == null) {
      return null;
    }
    return sdk.getName();
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }

  @Override
  @NotNull
  public String getPresentableName() {
    StringBuilder builder = new StringBuilder();

    if (myModuleExtension != null) {
      builder.append(ModuleExtensionProviderEP.findProvider(myModuleExtension.getId()).getName());

      builder.append(" : ");

      final Sdk sdk = myModuleExtension.getSdk();
      if(sdk == null) {
        builder.append(myModuleExtension.getSdkName());
      }
      else {
        builder.append(sdk.getName());
      }
    }
    else {
      builder.append(myModuleExtensionId);
    }

    return builder.toString();
  }

  @Override
  public boolean isValid() {
    return !isDisposed();
  }

  @Override
  public <R> R accept(@NotNull RootPolicy<R> policy, R initialValue) {
    return policy.visitModuleJdkOrderEntry(this, initialValue);
  }

  @Override
  public void writeExternal(@NotNull Element rootElement) throws WriteExternalException {
    final Element element = OrderEntryFactory.createOrderEntryElement(ENTRY_TYPE);
    element.setAttribute(EXTENSION_ID_ATTRIBUTE, myModuleExtensionId);
    rootElement.addContent(element);
  }

  @Override
  @NotNull
  public OrderEntry cloneEntry(@NotNull RootModelImpl rootModel,
                               ProjectRootManagerImpl projectRootManager,
                               VirtualFilePointerManager filePointerManager) {
    return new ModuleExtensionWithSdkOrderEntryImpl(this, rootModel,
                                                    ProjectRootManagerImpl.getInstanceImpl(getRootModel().getModule().getProject()));
  }

  @NotNull
  @Override
  public String getModuleExtensionId() {
    return myModuleExtensionId;
  }

  @Nullable
  @Override
  public ModuleExtensionWithSdk<?> getModuleExtension() {
    return myModuleExtension;
  }
}
