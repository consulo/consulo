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
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12:42/19.05.13
 */
public class ModuleExtensionWithSdkImpl<T extends ModuleExtensionWithSdk<T>> extends ModuleExtensionImpl<T> implements ModuleExtensionWithSdk<T> {
  protected String mySdkName;

  public ModuleExtensionWithSdkImpl(@NotNull String id, @NotNull Module module) {
    super(id, module);
  }

  @Override
  public void commit(@NotNull T mutableModuleExtension) {
    super.commit(mutableModuleExtension);

    mySdkName = mutableModuleExtension.getSdkName();
  }

  @Nullable
  @Override
  public Sdk getSdk() {
    if(mySdkName == null) {
      return null;
    }
    final ProjectSdksModel projectJdksModel = ProjectStructureConfigurable.getInstance(getModule().getProject()).getProjectJdksModel();
    return projectJdksModel.findSdk(mySdkName) ;
  }

  @Nullable
  @Override
  public String getSdkName() {
    return mySdkName;
  }
}
