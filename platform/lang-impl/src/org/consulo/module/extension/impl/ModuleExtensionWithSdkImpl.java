/*
 * Copyright 2013 Consulo.org
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
package org.consulo.module.extension.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.consulo.sdk.SdkUtil;
import org.consulo.util.pointers.NamedPointer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12:42/19.05.13
 */
public abstract class ModuleExtensionWithSdkImpl<T extends ModuleExtensionWithSdk<T>> extends ModuleExtensionImpl<T>
  implements ModuleExtensionWithSdk<T> {
  protected NamedPointer<Sdk> mySdkPointer;
  protected NamedPointer<Module> myModulePointer;

  public ModuleExtensionWithSdkImpl(@NotNull String id, @NotNull Module module) {
    super(id, module);
  }

  @Override
  public void commit(@NotNull T mutableModuleExtension) {
    super.commit(mutableModuleExtension);

    final String sdkName = mutableModuleExtension.getSdkName();
    mySdkPointer = sdkName == null ? null : SdkUtil.createPointer(sdkName);
  }

  protected void setSdkImpl(@Nullable Sdk sdk) {
    mySdkPointer = sdk == null ? null : SdkUtil.createPointer(sdk);
  }

  protected void setSdkInheritModuleImpl(@Nullable Module module) {
    myModulePointer = module == null ? null : ModuleUtilCore.createPointer(module);
  }

  public boolean isModifiedImpl(ModuleExtensionWithSdk<T> originExtension) {
    if (myIsEnabled != originExtension.isEnabled()) {
      return true;
    }
    if (!Comparing.equal(getSdkName(), originExtension.getSdkName())) {
      return true;
    }
    return !Comparing.equal(getSdkInheritModuleName(), originExtension.getSdkInheritModuleName());
  }

  @Nullable
  @Override
  public Sdk getSdk() {
    if(mySdkPointer == null) {
      return null;
    }
    return mySdkPointer.get();
  }

  @Nullable
  @Override
  public String getSdkName() {
    if(mySdkPointer == null) {
      return null;
    }
    return mySdkPointer.getName();
  }

  @Nullable
  @Override
  public Module getSdkInheritModule() {
    if(myModulePointer == null) {
      return null;
    }
    return myModulePointer.get();
  }

  @Nullable
  @Override
  public String getSdkInheritModuleName() {
    if(myModulePointer == null) {
      return null;
    }
    return myModulePointer.getName();
  }

  @Nullable
  @Override
  public SdkType getSdkType() {
    return SdkType.findInstance(getSdkTypeClass());
  }

  protected abstract Class<? extends SdkType> getSdkTypeClass();

  @Override
  protected void getStateImpl(@NotNull Element element) {
    element.setAttribute("sdk-name", StringUtil.notNullize(getSdkName()));
  }

  @Override
  protected void loadStateImpl(@NotNull Element element) {
    final String sdkName = StringUtil.nullize(element.getAttributeValue("sdk-name"));
    if(sdkName != null) {
      mySdkPointer = SdkUtil.createPointer(sdkName);
    }
  }
}
