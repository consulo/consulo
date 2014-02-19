/*
 * Copyright 2013 must-be.org
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
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.consulo.module.extension.ModuleInheritableNamedPointer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12:42/19.05.13
 */
public abstract class ModuleExtensionWithSdkImpl<T extends ModuleExtensionWithSdk<T>> extends ModuleExtensionImpl<T>
  implements ModuleExtensionWithSdk<T> {

  private ModuleInheritableNamedPointerImpl<Sdk> mySdkPointer;

  public ModuleExtensionWithSdkImpl(@NotNull String id, @NotNull Module module) {
    super(id, module);

    mySdkPointer = new SdkModuleInheritableNamedPointerImpl(module.getProject(), id);
  }

  @Override
  public void commit(@NotNull T mutableModuleExtension) {
    super.commit(mutableModuleExtension);

    mySdkPointer.set(mutableModuleExtension.getInheritableSdk());
  }

  @NotNull
  @Override
  public ModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
    return mySdkPointer;
  }

  @Nullable
  @Override
  public Sdk getSdk() {
    return getInheritableSdk().get();
  }

  @Nullable
  @Override
  public String getSdkName() {
    return getInheritableSdk().getName();
  }

  public boolean isModifiedImpl(ModuleExtensionWithSdk<T> originExtension) {
    if (myIsEnabled != originExtension.isEnabled()) {
      return true;
    }
    return !mySdkPointer.equals(originExtension.getInheritableSdk());
  }

  @Nullable
  @Override
  public SdkType getSdkType() {
    return SdkType.findInstance(getSdkTypeClass());
  }

  protected abstract Class<? extends SdkType> getSdkTypeClass();

  @Override
  protected void getStateImpl(@NotNull Element element) {
    super.getStateImpl(element);

    mySdkPointer.toXml(element);
  }

  @Override
  protected void loadStateImpl(@NotNull Element element) {
    super.loadStateImpl(element);

    mySdkPointer.fromXml(element);
  }
}
