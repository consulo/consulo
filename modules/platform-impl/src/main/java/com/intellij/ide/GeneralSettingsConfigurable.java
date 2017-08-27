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
package com.intellij.ide;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.components.JBRadioButton;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * To provide additional options in General section register implementation of {@link com.intellij.openapi.options.SearchableConfigurable} in the plugin.xml:
 * <p>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;generalOptionsProvider instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 */
public class GeneralSettingsConfigurable implements SearchableConfigurable, Configurable.NoScroll {

  private MyComponent myComponent;

  public GeneralSettingsConfigurable() {
    myComponent = new MyComponent();
  }

  private int getConfirmOpenNewProject() {
    if (myComponent.myConfirmWindowToOpenProject.isSelected()) {
      return GeneralSettings.OPEN_PROJECT_ASK;
    }
    else if (myComponent.myOpenProjectInNewWindow.isSelected()) {
      return GeneralSettings.OPEN_PROJECT_NEW_WINDOW;
    }
    else {
      return GeneralSettings.OPEN_PROJECT_SAME_WINDOW;
    }
  }

  private GeneralSettings.ProcessCloseConfirmation getProcessCloseConfirmation() {
    if (myComponent.myTerminateProcessJBRadioButton.isSelected()) {
      return GeneralSettings.ProcessCloseConfirmation.TERMINATE;
    }
    else if (myComponent.myDisconnectJBRadioButton.isSelected()) {
      return GeneralSettings.ProcessCloseConfirmation.DISCONNECT;
    }
    else {
      return GeneralSettings.ProcessCloseConfirmation.ASK;
    }
  }

  @RequiredDispatchThread
  @Override
  public void apply() throws ConfigurationException {
    GeneralSettings settings = GeneralSettings.getInstance();

    settings.setReopenLastProject(myComponent.myChkReopenLastProject.isSelected());
    settings.setSupportScreenReaders(myComponent.myChkSupportScreenReaders.isSelected());
    settings.setSyncOnFrameActivation(myComponent.myChkSyncOnFrameActivation.isSelected());
    settings.setSaveOnFrameDeactivation(myComponent.myChkSaveOnFrameDeactivation.isSelected());
    settings.setConfirmExit(myComponent.myConfirmExit.isSelected());
    settings.setConfirmOpenNewProject(getConfirmOpenNewProject());
    settings.setProcessCloseConfirmation(getProcessCloseConfirmation());

    settings.setAutoSaveIfInactive(myComponent.myChkAutoSaveIfInactive.isSelected());
    try {
      int newInactiveTimeout = Integer.parseInt(myComponent.myTfInactiveTimeout.getText());
      if (newInactiveTimeout > 0) {
        settings.setInactiveTimeout(newInactiveTimeout);
      }
    }
    catch (NumberFormatException ignored) {
    }
    settings.setUseSafeWrite(myComponent.myChkUseSafeWrite.isSelected());
  }

  @RequiredDispatchThread
  @Override
  public void reset() {
    GeneralSettings settings = GeneralSettings.getInstance();
    myComponent.myChkSupportScreenReaders.setSelected(settings.isSupportScreenReaders());
    myComponent.myChkReopenLastProject.setSelected(settings.isReopenLastProject());
    myComponent.myChkSyncOnFrameActivation.setSelected(settings.isSyncOnFrameActivation());
    myComponent.myChkSaveOnFrameDeactivation.setSelected(settings.isSaveOnFrameDeactivation());
    myComponent.myChkAutoSaveIfInactive.setSelected(settings.isAutoSaveIfInactive());
    myComponent.myTfInactiveTimeout.setText(Integer.toString(settings.getInactiveTimeout()));
    myComponent.myTfInactiveTimeout.setEditable(settings.isAutoSaveIfInactive());
    myComponent.myChkUseSafeWrite.setSelected(settings.isUseSafeWrite());
    myComponent.myConfirmExit.setSelected(settings.isConfirmExit());
    switch (settings.getProcessCloseConfirmation()) {
      case TERMINATE:
        myComponent.myTerminateProcessJBRadioButton.setSelected(true);
        break;
      case DISCONNECT:
        myComponent.myDisconnectJBRadioButton.setSelected(true);
        break;
      case ASK:
        myComponent.myAskJBRadioButton.setSelected(true);
        break;
    }
    switch (settings.getConfirmOpenNewProject()) {
      case GeneralSettings.OPEN_PROJECT_ASK:
        myComponent.myConfirmWindowToOpenProject.setSelected(true);
        break;
      case GeneralSettings.OPEN_PROJECT_NEW_WINDOW:
        myComponent.myOpenProjectInNewWindow.setSelected(true);
        break;
      case GeneralSettings.OPEN_PROJECT_SAME_WINDOW:
        myComponent.myOpenProjectInSameWindow.setSelected(true);
        break;
    }
  }

  @RequiredDispatchThread
  @Override
  public boolean isModified() {
    boolean isModified = false;
    GeneralSettings settings = GeneralSettings.getInstance();
    isModified |= settings.isReopenLastProject() != myComponent.myChkReopenLastProject.isSelected();
    isModified |= settings.isSupportScreenReaders() != myComponent.myChkSupportScreenReaders.isSelected();
    isModified |= settings.isSyncOnFrameActivation() != myComponent.myChkSyncOnFrameActivation.isSelected();
    isModified |= settings.isSaveOnFrameDeactivation() != myComponent.myChkSaveOnFrameDeactivation.isSelected();
    isModified |= settings.isAutoSaveIfInactive() != myComponent.myChkAutoSaveIfInactive.isSelected();
    isModified |= settings.isConfirmExit() != myComponent.myConfirmExit.isSelected();
    isModified |= settings.getProcessCloseConfirmation() != getProcessCloseConfirmation();
    isModified |= settings.getConfirmOpenNewProject() != getConfirmOpenNewProject();

    int inactiveTimeout = -1;
    try {
      inactiveTimeout = Integer.parseInt(myComponent.myTfInactiveTimeout.getText());
    }
    catch (NumberFormatException ignored) {
    }
    isModified |= inactiveTimeout > 0 && settings.getInactiveTimeout() != inactiveTimeout;

    isModified |= settings.isUseSafeWrite() != myComponent.myChkUseSafeWrite.isSelected();

    return isModified;
  }

  @RequiredDispatchThread
  @Override
  public JComponent createComponent() {
    if (myComponent == null) {
      myComponent = new MyComponent();
    }

    myComponent.myChkAutoSaveIfInactive.addChangeListener(e -> myComponent.myTfInactiveTimeout.setEditable(myComponent.myChkAutoSaveIfInactive.isSelected()));

    return myComponent.myPanel;
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("title.general");
  }

  @RequiredDispatchThread
  @Override
  public void disposeUIResources() {
    myComponent = null;
  }

  @Override
  @NotNull
  public String getHelpTopic() {
    return "preferences.general";
  }

  private static class MyComponent {
    private JPanel myPanel;
    private JCheckBox myChkReopenLastProject;
    private JCheckBox myChkSyncOnFrameActivation;
    private JCheckBox myChkSaveOnFrameDeactivation;
    private JCheckBox myChkAutoSaveIfInactive;
    private JTextField myTfInactiveTimeout;
    private JCheckBox myChkUseSafeWrite;
    private JCheckBox myConfirmExit;
    private JPanel myPluginOptionsPanel;
    private JBRadioButton myOpenProjectInNewWindow;
    private JBRadioButton myOpenProjectInSameWindow;
    private JBRadioButton myConfirmWindowToOpenProject;
    private JCheckBox myChkSupportScreenReaders;
    private JBRadioButton myTerminateProcessJBRadioButton;
    private JBRadioButton myDisconnectJBRadioButton;
    private JBRadioButton myAskJBRadioButton;

    public MyComponent() {
    }
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  @Nullable
  public Runnable enableSearch(String option) {
    return null;
  }
}
