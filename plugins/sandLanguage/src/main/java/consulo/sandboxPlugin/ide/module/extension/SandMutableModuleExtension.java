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
package consulo.sandboxPlugin.ide.module.extension;

import com.intellij.openapi.projectRoots.Sdk;
import consulo.roots.ModuleRootLayer;
import com.intellij.openapi.ui.VerticalFlowLayout;
import consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.extension.ui.ModuleExtensionSdkBoxBuilder;
import javax.annotation.Nonnull;

import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandMutableModuleExtension extends SandModuleExtension implements MutableModuleExtensionWithSdk<SandModuleExtension> {
  public SandMutableModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer rootModel) {
    super(id, rootModel);
  }

  @Nonnull
  @Override
  public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
    return (MutableModuleInheritableNamedPointer<Sdk>)super.getInheritableSdk();
  }

  @javax.annotation.Nullable
  @Override
  @RequiredUIAccess
  public JComponent createConfigurablePanel(@Nonnull Runnable updateOnCheck) {
    JPanel panel = new JPanel(new VerticalFlowLayout(true, false));
    panel.add(ModuleExtensionSdkBoxBuilder.createAndDefine(this, updateOnCheck).build());
    return panel;
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified(@Nonnull SandModuleExtension originalExtension) {
    return myIsEnabled != originalExtension.isEnabled();
  }
}
