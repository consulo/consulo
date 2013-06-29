/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.run;

import com.intellij.execution.configurations.LogFileOptions;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.SdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.util.Condition;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.SimpleTextAttributes;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.sdk.ConsuloSdkType;

import javax.swing.*;
import java.util.ArrayList;

@Logger
public class PluginRunConfigurationEditor extends SettingsEditor<PluginRunConfiguration> {
  private static class ArtifactItem {
    private final String myName;
    private final Artifact myArtifact;

    private ArtifactItem(String name, Artifact artifact) {
      myName = name;
      myArtifact = artifact;
    }
  }

  private SdkComboBox myJavaSdkComboBox;
  private SdkComboBox myConsuloSdkComboBox;
  private RawCommandLineEditor myProgramParameters;
  private RawCommandLineEditor myVMParameters;
  private JCheckBox myShowIdeaLogCheckBox;
  private JComboBox myArtifactComboBox;
  private JPanel myRoot;

  private final Project myProject;

  public PluginRunConfigurationEditor(Project project) {
    myProject = project;
  }

  private static void setShow(PluginRunConfiguration prc, boolean show) {
    final ArrayList<LogFileOptions> logFiles = prc.getLogFiles();
    for (LogFileOptions logFile : logFiles) {
      logFile.setEnable(show);
    }
  }

  private static boolean isShow(PluginRunConfiguration prc) {
    final ArrayList<LogFileOptions> logFiles = prc.getLogFiles();
    for (LogFileOptions logFile : logFiles) {
      if (logFile.isEnabled()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void resetEditorFrom(PluginRunConfiguration prc) {
    myShowIdeaLogCheckBox.setSelected(isShow(prc));

    myVMParameters.setText(prc.VM_PARAMETERS);
    myProgramParameters.setText(prc.PROGRAM_PARAMETERS);

    final String artifactName = prc.getArtifactName();
    if(artifactName != null) {
      final Artifact artifact = ArtifactManager.getInstance(myProject).findArtifact(artifactName);
      if(artifact != null) {
        int i = -1;
        for(int l = 0; l < myArtifactComboBox.getItemCount(); l ++) {
          final ArtifactItem itemAt = (ArtifactItem) myArtifactComboBox.getItemAt(l);
          if(itemAt.myArtifact == artifact) {
            i = l;
            break;
          }
        }

        if(i >= 0) {
          myArtifactComboBox.setSelectedIndex(i);
        }
        else {
          myArtifactComboBox.addItem(new ArtifactItem(artifactName, artifact));
        }
      }
      else {
        myArtifactComboBox.addItem(new ArtifactItem(artifactName, null));
      }
    }
    else {
      myArtifactComboBox.setSelectedItem(null);
    }

    myJavaSdkComboBox.setSelectedSdk(prc.getJavaSdkName());
    myConsuloSdkComboBox.setSelectedSdk(prc.getConsuloSdkName());
  }

  @Override
  public void applyEditorTo(PluginRunConfiguration prc) throws ConfigurationException {
    setShow(prc, myShowIdeaLogCheckBox.isSelected());

    prc.setArtifactName(myArtifactComboBox.getSelectedItem() == null ? null : ((ArtifactItem)myArtifactComboBox.getSelectedItem()).myName);
    prc.setJavaSdkName(myJavaSdkComboBox.getSelectedSdkName());
    prc.setConsuloSdkName(myConsuloSdkComboBox.getSelectedSdkName());

    prc.VM_PARAMETERS = myVMParameters.getText();
    prc.PROGRAM_PARAMETERS = myProgramParameters.getText();
  }

  @Override
  @NotNull
  public JComponent createEditor() {
    return myRoot;
  }

  @Override
  public void disposeEditor() {
  }

  private void createUIComponents() {
    ProjectSdksModel projectSdksModel = new ProjectSdksModel();
    if (!projectSdksModel.isInitialized()) {
      projectSdksModel.reset(myProject);
    }
    myJavaSdkComboBox = new SdkComboBox(projectSdksModel, new Condition<SdkTypeId>() {
      @Override
      public boolean value(SdkTypeId sdkTypeId) {
        return sdkTypeId instanceof JavaSdk;
      }
    }, false);

    myConsuloSdkComboBox = new SdkComboBox(projectSdksModel, new Condition<SdkTypeId>() {
      @Override
      public boolean value(SdkTypeId sdkTypeId) {
        return sdkTypeId instanceof ConsuloSdkType;
      }
    }, false);

    final Artifact[] sortedArtifacts = ArtifactManager.getInstance(myProject).getSortedArtifacts();
    myArtifactComboBox = new JComboBox();
    for (Artifact sortedArtifact : sortedArtifacts) {
      myArtifactComboBox.addItem(new ArtifactItem(sortedArtifact.getName(), sortedArtifact));
    }

    myArtifactComboBox.setRenderer(new ColoredListCellRenderer() {
      @Override
      protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        ArtifactItem artifactItem = (ArtifactItem)value;
        if(artifactItem == null) {
          append("<None>", SimpleTextAttributes.ERROR_ATTRIBUTES);
          return;
        }

        final Artifact artifact = artifactItem.myArtifact;
        if (artifact == null) {
          append(artifactItem.myName, SimpleTextAttributes.ERROR_ATTRIBUTES);
          setIcon(AllIcons.Nodes.Artifact);
        }
        else {
          append(artifactItem.myName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
          setIcon(artifact.getArtifactType().getIcon());
        }
      }
    });
  }
}
