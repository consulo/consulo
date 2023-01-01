/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem.fileWatcher.impl.ui;

import consulo.execution.ui.CommonProgramParametersLayout;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.FileChooserTextBoxBuilder;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsParameters;
import consulo.virtualFileSystem.fileWatcher.impl.BackgroundTaskByVfsParametersImpl;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
public class BackgroundTaskByVfsChangeLayout extends CommonProgramParametersLayout<BackgroundTaskByVfsParameters> {
  private CheckBox myShowLogCheckBox;
  private FileChooserTextBoxBuilder.Controller myExePathController;
  private FileChooserTextBoxBuilder.Controller myOutputPathController;

  public BackgroundTaskByVfsChangeLayout(@Nonnull DialogService dialogService) {
    super(dialogService);
  }

  @RequiredUIAccess
  @Override
  protected void addBefore(@Nonnull FormBuilder builder) {
    FileChooserTextBoxBuilder exePathBuilder = FileChooserTextBoxBuilder.create(getProject());
    exePathBuilder.fileChooserDescriptor(new FileChooserDescriptor(false, true, false, false, false, false));
    exePathBuilder.dialogTitle(LocalizeValue.localizeTODO("Select Executable"));
    myExePathController = exePathBuilder.build();

    builder.addLabeled(LocalizeValue.localizeTODO("Executable Path"), myExePathController.getComponent());

    super.addBefore(builder);
  }

  @RequiredUIAccess
  @Override
  protected void addAfter(@Nonnull FormBuilder builder) {
    FileChooserTextBoxBuilder outputPathBuilder = FileChooserTextBoxBuilder.create(getProject());
    outputPathBuilder.fileChooserDescriptor(new FileChooserDescriptor(false, true, false, false, false, false));
    outputPathBuilder.dialogTitle(LocalizeValue.localizeTODO("Select Output Path"));
    myOutputPathController = outputPathBuilder.build();
    builder.addLabeled(LocalizeValue.localizeTODO("Output Path"), myOutputPathController.getComponent());

    myShowLogCheckBox = CheckBox.create(LocalizeValue.localizeTODO("Show console"));
    builder.addBottom(myShowLogCheckBox);
  }

  @RequiredUIAccess
  @Override
  public void apply(BackgroundTaskByVfsParameters configuration) {
    super.apply(configuration);
    configuration.setShowConsole(myShowLogCheckBox.getValueOrError());
    configuration.setExePath(FileUtil.toSystemIndependentName(StringUtil.notNullize(myExePathController.getValue())));
    configuration.setOutPath(FileUtil.toSystemIndependentName(StringUtil.notNullize(myOutputPathController.getValue())));
  }

  @RequiredUIAccess
  @Override
  public void reset(BackgroundTaskByVfsParameters configuration) {
    super.reset(configuration);
    myShowLogCheckBox.setValue(configuration.isShowConsole());
    myExePathController.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(configuration.getExePath())));
    myOutputPathController.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(configuration.getOutPath())));

    getComponent().setEnabledRecursive(configuration != BackgroundTaskByVfsParametersImpl.EMPTY);
  }
}
