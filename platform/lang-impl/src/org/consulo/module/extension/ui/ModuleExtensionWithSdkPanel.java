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
package org.consulo.module.extension.ui;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.util.Condition;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 14:43/19.05.13
 */
public class ModuleExtensionWithSdkPanel extends JPanel {
  private final MutableModuleExtensionWithSdk<?> myExtensionWithSdk;
  private JPanel myRoot;
  private SdkComboBox mySdkComboBox;

  public ModuleExtensionWithSdkPanel(MutableModuleExtensionWithSdk<?> extensionWithSdk) {
    myExtensionWithSdk = extensionWithSdk;
  }

  private void createUIComponents() {
    myRoot = this;

    final SdkType sdkType = myExtensionWithSdk.getSdkType();
    final ProjectSdksModel projectJdksModel = ProjectStructureConfigurable.getInstance(myExtensionWithSdk.getModule().getProject()).getProjectJdksModel();
    mySdkComboBox = new SdkComboBox(projectJdksModel, new Condition<SdkTypeId>() {
      @Override
      public boolean value(SdkTypeId sdkTypeId) {
        return sdkTypeId == sdkType;
      }
    });

    final Sdk sdk = myExtensionWithSdk.getSdk();
    if(sdk == null) {
      mySdkComboBox.setInvalidJdk(myExtensionWithSdk.getSdkName());
    }
    else {
      mySdkComboBox.setSelectedJdk(sdk);
    }

    mySdkComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
       myExtensionWithSdk.setSdk(mySdkComboBox.getSelectedJdk());
      }
    });
  }
}
