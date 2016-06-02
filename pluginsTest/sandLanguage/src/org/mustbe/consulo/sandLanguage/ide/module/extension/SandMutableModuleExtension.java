/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.sandLanguage.ide.module.extension;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.ui.VerticalFlowLayout;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.consulo.module.extension.ui.ModuleExtensionSdkBoxBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandMutableModuleExtension extends SandModuleExtension implements MutableModuleExtensionWithSdk<SandModuleExtension> {
  public SandMutableModuleExtension(@NotNull String id, @NotNull ModuleRootLayer rootModel) {
    super(id, rootModel);
  }

  @NotNull
  @Override
  public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
    return (MutableModuleInheritableNamedPointer<Sdk>)super.getInheritableSdk();
  }

  @Nullable
  @Override
  @RequiredDispatchThread
  public JComponent createConfigurablePanel(@NotNull Runnable updateOnCheck) {
    JPanel panel = new JPanel(new VerticalFlowLayout(true, false));
    panel.add(ModuleExtensionSdkBoxBuilder.createAndDefine(this, updateOnCheck).build());
    return panel;
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified(@NotNull SandModuleExtension originalExtension) {
    return myIsEnabled != originalExtension.isEnabled();
  }
}
