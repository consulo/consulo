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

import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;

import javax.swing.*;

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

    final ProjectSdksModel projectJdksModel = ProjectStructureConfigurable.getInstance(myExtensionWithSdk.getModule().getProject()).getProjectJdksModel();
    mySdkComboBox = new SdkComboBox(projectJdksModel);
  }
}
