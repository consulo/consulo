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
package org.consulo.module.extension.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.util.Condition;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.DeprecationInfo;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 14:43/19.05.13
 */
@Deprecated
@DeprecationInfo(value = "Use ModuleExtensionSdkBoxBuilder", until = "2.0")
public class ModuleExtensionWithSdkPanel extends JPanel {
  private final MutableModuleExtensionWithSdk<?> myExtensionWithSdk;
  @Nullable
  private final Runnable myClasspathStateUpdater;
  private JPanel myRoot;
  private SdkComboBox mySdkComboBox;

  public ModuleExtensionWithSdkPanel(@NotNull MutableModuleExtensionWithSdk<?> extensionWithSdk, @Nullable Runnable classpathStateUpdater) {
    myExtensionWithSdk = extensionWithSdk;
    myClasspathStateUpdater = classpathStateUpdater;
  }

  private void createUIComponents() {
    myRoot = this;

    final Class<? extends SdkType> sdkType = myExtensionWithSdk.getSdkTypeClass();
    final ProjectSdksModel projectSdksModel =
            ProjectStructureConfigurable.getInstance(myExtensionWithSdk.getModule().getProject()).getProjectSdksModel();
    mySdkComboBox = new SdkComboBox(projectSdksModel, new Condition<SdkTypeId>() {
      @Override
      public boolean value(SdkTypeId sdkTypeId) {
        return sdkType.isAssignableFrom(sdkTypeId.getClass());
      }
    }, true);
    mySdkComboBox.insertModuleItems(myExtensionWithSdk);

    final MutableModuleInheritableNamedPointer<Sdk> inheritableSdk = getInheritableSdk();
    if (inheritableSdk.isNull()) {
      mySdkComboBox.setSelectedNoneSdk();
    }
    else {
      final String sdkInheritModuleName = inheritableSdk.getModuleName();
      if (sdkInheritModuleName != null) {
        final Module sdkInheritModule = inheritableSdk.getModule();
        if (sdkInheritModule == null) {
          mySdkComboBox.addInvalidModuleItem(sdkInheritModuleName);
        }
        mySdkComboBox.setSelectedModule(sdkInheritModuleName);
      }
      else {
        mySdkComboBox.setSelectedSdk(inheritableSdk.getName());
      }
    }

    mySdkComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        inheritableSdk.set(mySdkComboBox.getSelectedModuleName(), mySdkComboBox.getSelectedSdkName());

        if (myClasspathStateUpdater != null) {
          SwingUtilities.invokeLater(myClasspathStateUpdater);
        }
      }
    });
  }

  /**
   * This is need for override - when sdk available via profiling
   * @return
   */
  @NotNull
  public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
    return myExtensionWithSdk.getInheritableSdk();
  }
}
