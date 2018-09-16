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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NotNullComputable;
import com.intellij.util.ObjectUtil;
import consulo.fileChooser.FileOperateDialogSettings;
import consulo.options.SimpleConfigurable;
import consulo.ui.*;
import consulo.ui.fileOperateDialog.FileChooseDialogProvider;
import consulo.ui.fileOperateDialog.FileOperateDialogProvider;
import consulo.ui.fileOperateDialog.FileSaveDialogProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GeneralSettingsConfigurable extends SimpleConfigurable<GeneralSettingsConfigurable.MyComponent> implements SearchableConfigurable {

  protected static class MyComponent implements NotNullComputable<Component> {
    private CheckBox myChkReopenLastProject;
    private CheckBox myConfirmExit;

    private RadioButton myOpenProjectInNewWindow;
    private RadioButton myOpenProjectInSameWindow;
    private RadioButton myConfirmWindowToOpenProject;

    private CheckBox myChkSyncOnFrameActivation;
    private CheckBox myChkSaveOnFrameDeactivation;
    private CheckBox myChkAutoSaveIfInactive;
    private TextBox myTfInactiveTimeout;
    private CheckBox myChkUseSafeWrite;

    private CheckBox myChkSupportScreenReaders;

    private RadioButton myTerminateProcessRadioButton;
    private RadioButton myDisconnectRadioButton;
    private RadioButton myAskRadioButton;

    private ComboBox<FileOperateDialogProvider> myFileChooseDialogBox;
    private ComboBox<FileOperateDialogProvider> myFileSaveDialogBox;

    private VerticalLayout myRootLayout;

    @RequiredUIAccess
    public MyComponent() {
      myRootLayout = VerticalLayout.create();

      VerticalLayout startupOrShutdownLayout = VerticalLayout.create();
      startupOrShutdownLayout.add(myChkReopenLastProject = CheckBox.create(IdeBundle.message("checkbox.reopen.last.project.on.startup")));
      startupOrShutdownLayout.add(myConfirmExit = CheckBox.create(IdeBundle.message("checkbox.confirm.application.exit")));
      myRootLayout.add(LabeledLayout.create("Startup/Shutdown", startupOrShutdownLayout));

      VerticalLayout projectReopeningLayout = VerticalLayout.create();
      ValueGroup<Boolean> projectOpenGroup = ValueGroup.createBool();
      projectReopeningLayout.add(myOpenProjectInNewWindow = RadioButton.create("Open project in new window").toGroup(projectOpenGroup));
      projectReopeningLayout.add(myOpenProjectInSameWindow = RadioButton.create("Open project in the same window").toGroup(projectOpenGroup));
      projectReopeningLayout.add(myConfirmWindowToOpenProject = RadioButton.create("Confirm window to open project in").toGroup(projectOpenGroup));
      myRootLayout.add(LabeledLayout.create("Project Opening", projectReopeningLayout));

      VerticalLayout syncLayout = VerticalLayout.create();
      syncLayout.add(myChkSyncOnFrameActivation = CheckBox.create(IdeBundle.message("checkbox.synchronize.files.on.frame.activation")));
      syncLayout.add(myChkSaveOnFrameDeactivation = CheckBox.create(IdeBundle.message("checkbox.save.files.on.frame.deactivation")));

      HorizontalLayout syncWithIde = HorizontalLayout.create();
      syncLayout.add(syncWithIde);
      syncWithIde.add(myChkAutoSaveIfInactive = CheckBox.create(IdeBundle.message("checkbox.save.files.automatically")));
      syncWithIde.add(myTfInactiveTimeout = TextBox.create());
      myTfInactiveTimeout.setEnabled(false);
      syncWithIde.add(Label.create(IdeBundle.message("label.inactive.timeout.sec")));
      myChkAutoSaveIfInactive.addValueListener(event -> myTfInactiveTimeout.setEnabled(event.getValue()));

      syncLayout.add(myChkUseSafeWrite = CheckBox.create("Use \"safe write\" (save changes to a temporary file first)"));
      myRootLayout.add(LabeledLayout.create("Synchronization", syncLayout));

      VerticalLayout screenLayout = VerticalLayout.create();
      screenLayout.add(myChkSupportScreenReaders = CheckBox.create(IdeBundle.message("checkbox.support.screen.readers")));
      myRootLayout.add(LabeledLayout.create("Accessibility", screenLayout));

      VerticalLayout processLayout = VerticalLayout.create();
      ValueGroup<Boolean> processGroup = ValueGroup.createBool();
      processLayout.add(myTerminateProcessRadioButton = RadioButton.create(IdeBundle.message("radio.process.close.terminate")).toGroup(processGroup));
      processLayout.add(myDisconnectRadioButton = RadioButton.create(IdeBundle.message("radio.process.close.disconnect")).toGroup(processGroup));
      processLayout.add(myAskRadioButton = RadioButton.create(IdeBundle.message("radio.process.close.ask")).toGroup(processGroup));

      myRootLayout.add(LabeledLayout.create(IdeBundle.message("group.settings.process.tab.close"), processLayout));

      HorizontalLayout fileDialogsLayout = HorizontalLayout.create(10);

      ComboBox.Builder<FileOperateDialogProvider> fileChooseDialogBox = ComboBox.<FileOperateDialogProvider>builder();
      for (FileChooseDialogProvider fileChooseDialogProvider : FileChooseDialogProvider.EP_NAME.getExtensions()) {
        if (fileChooseDialogProvider.isAvaliable()) {
          fileChooseDialogBox.add(fileChooseDialogProvider, fileChooseDialogProvider.getName());
        }
      }

      fileDialogsLayout.add(LabeledComponents.left("File/Path Choose Dialog Type", myFileChooseDialogBox = fileChooseDialogBox.build()));

      ComboBox.Builder<FileOperateDialogProvider> fileSaveDialogBox = ComboBox.<FileOperateDialogProvider>builder();
      for (FileSaveDialogProvider fileSaveDialogProvider : FileSaveDialogProvider.EP_NAME.getExtensions()) {
        if (fileSaveDialogProvider.isAvaliable()) {
          fileSaveDialogBox.add(fileSaveDialogProvider, fileSaveDialogProvider.getName());
        }
      }

      fileDialogsLayout.add(LabeledComponents.left("File Save Dialog Type", myFileSaveDialogBox = fileSaveDialogBox.build()));

      myRootLayout.add(LabeledLayout.create("File Dialogs", fileDialogsLayout));
    }

    @Nonnull
    @Override
    public Component compute() {
      return myRootLayout;
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected MyComponent createPanel() {
    return new MyComponent();
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull MyComponent component) {
    boolean isModified = false;
    GeneralSettings settings = GeneralSettings.getInstance();
    isModified |= settings.isReopenLastProject() != component.myChkReopenLastProject.getValue();
    isModified |= settings.isConfirmExit() != component.myConfirmExit.getValue();
    isModified |= settings.isSupportScreenReaders() != component.myChkSupportScreenReaders.getValue();
    isModified |= settings.isSyncOnFrameActivation() != component.myChkSyncOnFrameActivation.getValue();
    isModified |= settings.isSaveOnFrameDeactivation() != component.myChkSaveOnFrameDeactivation.getValue();
    isModified |= settings.isAutoSaveIfInactive() != component.myChkAutoSaveIfInactive.getValue();
    isModified |= settings.getProcessCloseConfirmation() != getProcessCloseConfirmation(component);
    isModified |= settings.getConfirmOpenNewProject() != getConfirmOpenNewProject(component);

    FileOperateDialogSettings dialogSettings = FileOperateDialogSettings.getInstance();
    isModified |= isModified(dialogSettings.getFileChooseDialogId(), component.myFileChooseDialogBox);
    isModified |= isModified(dialogSettings.getFileSaveDialogId(), component.myFileSaveDialogBox);

    int inactiveTimeout = -1;
    try {
      inactiveTimeout = Integer.parseInt(component.myTfInactiveTimeout.getValue());
    }
    catch (NumberFormatException ignored) {
    }
    isModified |= inactiveTimeout > 0 && settings.getInactiveTimeout() != inactiveTimeout;

    isModified |= settings.isUseSafeWrite() != component.myChkUseSafeWrite.getValue();

    return isModified;
  }

  private boolean isModified(@Nullable String id, ComboBox<FileOperateDialogProvider> comboBox) {
    FileOperateDialogProvider value = comboBox.getValueOrError();

    if (id == null && FileOperateDialogProvider.APPLICATION_ID.equals(value.getId())) {
      return false;
    }

    return !Objects.equals(id, value.getId());
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull MyComponent component) throws ConfigurationException {
    GeneralSettings settings = GeneralSettings.getInstance();

    settings.setReopenLastProject(component.myChkReopenLastProject.getValue());
    settings.setSupportScreenReaders(component.myChkSupportScreenReaders.getValue());
    settings.setSyncOnFrameActivation(component.myChkSyncOnFrameActivation.getValue());
    settings.setSaveOnFrameDeactivation(component.myChkSaveOnFrameDeactivation.getValue());
    settings.setConfirmExit(component.myConfirmExit.getValue());
    settings.setConfirmOpenNewProject(getConfirmOpenNewProject(component));
    settings.setProcessCloseConfirmation(getProcessCloseConfirmation(component));

    settings.setAutoSaveIfInactive(component.myChkAutoSaveIfInactive.getValue());
    try {
      int newInactiveTimeout = Integer.parseInt(component.myTfInactiveTimeout.getValue());
      if (newInactiveTimeout > 0) {
        settings.setInactiveTimeout(newInactiveTimeout);
      }
    }
    catch (NumberFormatException ignored) {
    }
    settings.setUseSafeWrite(component.myChkUseSafeWrite.getValue());

    FileOperateDialogSettings dialogSettings = FileOperateDialogSettings.getInstance();

    apply(component.myFileChooseDialogBox, dialogSettings::setFileChooseDialogId);
    apply(component.myFileSaveDialogBox, dialogSettings::setFileSaveDialogId);
  }

  private void apply(ComboBox<FileOperateDialogProvider> comboBox, Consumer<String> func) {
    FileOperateDialogProvider value = comboBox.getValueOrError();

    String id = value.getId();
    if (FileOperateDialogProvider.APPLICATION_ID.equals(id)) {
      func.accept(null);
      return;
    }

    func.accept(id);
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull MyComponent component) {
    GeneralSettings settings = GeneralSettings.getInstance();
    component.myChkSupportScreenReaders.setValue(settings.isSupportScreenReaders());
    component.myChkReopenLastProject.setValue(settings.isReopenLastProject());
    component.myChkSyncOnFrameActivation.setValue(settings.isSyncOnFrameActivation());
    component.myChkSaveOnFrameDeactivation.setValue(settings.isSaveOnFrameDeactivation());
    component.myChkAutoSaveIfInactive.setValue(settings.isAutoSaveIfInactive());
    component.myTfInactiveTimeout.setValue(Integer.toString(settings.getInactiveTimeout()));
    component.myTfInactiveTimeout.setEnabled(settings.isAutoSaveIfInactive());
    component.myChkUseSafeWrite.setValue(settings.isUseSafeWrite());
    component.myConfirmExit.setValue(settings.isConfirmExit());
    switch (settings.getProcessCloseConfirmation()) {
      case TERMINATE:
        component.myTerminateProcessRadioButton.setValue(true);
        break;
      case DISCONNECT:
        component.myDisconnectRadioButton.setValue(true);
        break;
      case ASK:
        component.myAskRadioButton.setValue(true);
        break;
    }
    switch (settings.getConfirmOpenNewProject()) {
      case GeneralSettings.OPEN_PROJECT_ASK:
        component.myConfirmWindowToOpenProject.setValue(true);
        break;
      case GeneralSettings.OPEN_PROJECT_NEW_WINDOW:
        component.myOpenProjectInNewWindow.setValue(true);
        break;
      case GeneralSettings.OPEN_PROJECT_SAME_WINDOW:
        component.myOpenProjectInSameWindow.setValue(true);
        break;
    }

    FileOperateDialogSettings dialogSettings = FileOperateDialogSettings.getInstance();

    reset(component.myFileChooseDialogBox, dialogSettings::getFileChooseDialogId);
    reset(component.myFileSaveDialogBox, dialogSettings::getFileSaveDialogId);
  }

  @RequiredUIAccess
  private void reset(ComboBox<FileOperateDialogProvider> comboBox, Supplier<String> idGetter) {
    String id = ObjectUtil.notNull(idGetter.get(), FileOperateDialogProvider.APPLICATION_ID);

    comboBox.setValueByCondition(it -> it.getId().equals(id));
  }

  private static int getConfirmOpenNewProject(MyComponent component) {
    if (component.myConfirmWindowToOpenProject.getValue()) {
      return GeneralSettings.OPEN_PROJECT_ASK;
    }
    else if (component.myOpenProjectInNewWindow.getValue()) {
      return GeneralSettings.OPEN_PROJECT_NEW_WINDOW;
    }
    else {
      return GeneralSettings.OPEN_PROJECT_SAME_WINDOW;
    }
  }

  private static GeneralSettings.ProcessCloseConfirmation getProcessCloseConfirmation(MyComponent component) {
    if (component.myTerminateProcessRadioButton.getValue()) {
      return GeneralSettings.ProcessCloseConfirmation.TERMINATE;
    }
    else if (component.myDisconnectRadioButton.getValue()) {
      return GeneralSettings.ProcessCloseConfirmation.DISCONNECT;
    }
    else {
      return GeneralSettings.ProcessCloseConfirmation.ASK;
    }
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("title.general");
  }

  @Override
  @Nonnull
  public String getHelpTopic() {
    return "preferences.general";
  }

  @Override
  @Nonnull
  public String getId() {
    return getHelpTopic();
  }
}
