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

package consulo.module.impl.internal.layer.orderEntry;

import consulo.content.RootProvider;
import consulo.content.bundle.Sdk;
import consulo.module.content.internal.ProjectRootManagerImpl;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author dsl
 */
public class ModuleExtensionWithSdkOrderEntryImpl extends LibraryOrderEntryBaseImpl implements ClonableOrderEntry, ModuleExtensionWithSdkOrderEntry {
  private String myModuleExtensionId;

  public ModuleExtensionWithSdkOrderEntryImpl(@Nonnull String moduleExtensionId, @Nonnull ModuleRootLayerImpl rootModel) {
    this(moduleExtensionId, rootModel, true);
  }

  public ModuleExtensionWithSdkOrderEntryImpl(@Nonnull String moduleExtensionId, @Nonnull ModuleRootLayerImpl rootModel, boolean init) {
    super(ModuleExtensionWithSdkOrderEntryType.getInstance(), rootModel, ProjectRootManagerImpl.getInstanceImpl(rootModel.getProject()));
    myModuleExtensionId = moduleExtensionId;
    if (init) {
      init();

      myProjectRootManagerImpl.addOrderWithTracking(this);
    }
  }

  @Override
  protected RootProvider getRootProvider() {
    Sdk sdk = getSdk();
    if(sdk == null) {
      return null;
    }
    return sdk.getRootProvider();
  }

  @Override
  @Nullable
  public Sdk getSdk() {
    ModuleExtensionWithSdk<?> moduleExtension = getModuleExtension();
    if (moduleExtension == null) {
      return null;
    }
    return myModuleRootLayer.getRootModel().getConfigurationAccessor().getSdk(moduleExtension.getSdk(),
                                                                              moduleExtension.getSdkName());
  }

  @Override
  @Nullable
  public String getSdkName() {
    ModuleExtensionWithSdk<?> moduleExtension = getModuleExtension();
    if (moduleExtension == null) {
      return null;
    }
    return moduleExtension.getSdkName();
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }

  @Override
  @Nonnull
  public String getPresentableName() {
    StringBuilder builder = new StringBuilder();

    ModuleExtensionWithSdk<?> moduleExtension = getModuleExtension();
    if (moduleExtension != null) {
      Sdk sdk = moduleExtension.getSdk();
      if (sdk == null) {
        builder.append(moduleExtension.getSdkName());
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
  public void dispose() {
    super.dispose();

    myProjectRootManagerImpl.removeOrderWithTracking(this);
  }

  @Override
  public boolean isValid() {
    return !isDisposed();
  }

  @Override
  public <R> R accept(@Nonnull RootPolicy<R> policy, R initialValue) {
    return policy.visitModuleExtensionSdkOrderEntry(this, initialValue);
  }

  @Override
  public boolean isEquivalentTo(@Nonnull OrderEntry other) {
    if (other instanceof ModuleExtensionWithSdkOrderEntry) {
      String name1 = getSdkName();
      String name2 = ((ModuleExtensionWithSdkOrderEntry)other).getSdkName();
      return Comparing.strEqual(name1, name2);
    }
    return false;
  }

  @Override
  @Nonnull
  public OrderEntry cloneEntry(@Nonnull ModuleRootLayerImpl rootModel) {
    return new ModuleExtensionWithSdkOrderEntryImpl(myModuleExtensionId, rootModel, true);
  }

  @Nonnull
  @Override
  public String getModuleExtensionId() {
    return myModuleExtensionId;
  }

  @Nullable
  @Override
  public ModuleExtensionWithSdk<?> getModuleExtension() {
    ModuleExtension<?> extensionWithoutCheck = myModuleRootLayer.getExtensionWithoutCheck(myModuleExtensionId);
    if (!(extensionWithoutCheck instanceof ModuleExtensionWithSdk)) {
      return null;
    }
    return (ModuleExtensionWithSdk) extensionWithoutCheck;
  }
}
