/*
 * Copyright 2013-2016 consulo.io
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
package consulo.module.extension.impl;

import com.intellij.openapi.projectRoots.Sdk;
import consulo.annotation.access.RequiredReadAction;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.extension.ModuleInheritableNamedPointer;
import consulo.roots.ModuleRootLayer;
import org.jdom.Element;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12:42/19.05.13
 */
public abstract class ModuleExtensionWithSdkImpl<T extends ModuleExtensionWithSdk<T>> extends ModuleExtensionImpl<T> implements ModuleExtensionWithSdk<T> {

  private ModuleInheritableNamedPointerImpl<Sdk> mySdkPointer;

  public ModuleExtensionWithSdkImpl(@Nonnull String id, @Nonnull ModuleRootLayer rootLayer) {
    super(id, rootLayer);

    mySdkPointer = new SdkModuleInheritableNamedPointerImpl(rootLayer, id);
  }

  @RequiredReadAction
  @Override
  public void commit(@Nonnull T mutableModuleExtension) {
    super.commit(mutableModuleExtension);

    mySdkPointer.set(mutableModuleExtension.getInheritableSdk());
  }

  @Nonnull
  @Override
  public ModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
    return mySdkPointer;
  }

  @javax.annotation.Nullable
  @Override
  public Sdk getSdk() {
    return getInheritableSdk().get();
  }

  @javax.annotation.Nullable
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

  @Override
  protected void getStateImpl(@Nonnull Element element) {
    super.getStateImpl(element);

    mySdkPointer.toXml(element);
  }

  @Override
  @RequiredReadAction
  protected void loadStateImpl(@Nonnull Element element) {
    super.loadStateImpl(element);

    mySdkPointer.fromXml(element);
  }
}
