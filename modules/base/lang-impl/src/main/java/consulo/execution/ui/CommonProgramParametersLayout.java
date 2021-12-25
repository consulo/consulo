/*
 * Copyright 2013-2021 consulo.io
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
package consulo.execution.ui;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton;
import com.intellij.icons.AllIcons;
import com.intellij.ide.macro.Macro;
import com.intellij.ide.macro.MacrosDialog;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathUtil;
import com.intellij.util.execution.ParametersListUtil;
import consulo.ide.ui.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.dialog.DialogService;
import consulo.ui.util.FormBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25/12/2021
 * <p>
 * Unified implementation of {@link com.intellij.execution.ui.CommonProgramParametersPanel}
 */
public class CommonProgramParametersLayout<P extends CommonProgramRunConfigurationParameters> implements PseudoComponent {
  private FileChooserTextBoxBuilder.Controller myWorkDirectoryBox;
  private EnvironmentVariablesTextFieldWithBrowseButton myEnvVariablesComponent;
  private TextBoxWithExpandAction myProgramParametersComponent;

  private Module myModuleContext = null;
  private boolean myHasModuleMacro;

  private Component myLayout;

  @Nonnull
  protected final DialogService myDialogService;

  public CommonProgramParametersLayout(@Nonnull DialogService dialogService) {
    myDialogService = dialogService;
  }

  @RequiredUIAccess
  public void build() {
    FormBuilder builder = FormBuilder.create();

    addBefore(builder);

    myProgramParametersComponent = TextBoxWithExpandAction.create(AllIcons.Actions.ShowViewer, "", ParametersListUtil.DEFAULT_LINE_PARSER, ParametersListUtil.DEFAULT_LINE_JOINER);
    builder.addLabeled(ExecutionBundle.message("run.configuration.program.parameters"), myProgramParametersComponent);

    FileChooserTextBoxBuilder workDirBuilder = FileChooserTextBoxBuilder.create(getProject());
    workDirBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());
    workDirBuilder.dialogTitle(ExecutionBundle.message("select.working.directory.message"));
    workDirBuilder.dialogDescription(LocalizeValue.of());

    myWorkDirectoryBox = workDirBuilder.build();
    myWorkDirectoryBox.getComponent()
            .addFirstExtension(new TextBoxWithExtensions.Extension(false, PlatformIconGroup.generalInlineVariables(), PlatformIconGroup.generalInlineVariablesHover(), event -> showMacroDialog()));

    builder.addLabeled(ExecutionBundle.message("run.configuration.working.directory.label"), myWorkDirectoryBox.getComponent());

    myEnvVariablesComponent = new EnvironmentVariablesTextFieldWithBrowseButton();
    builder.addLabeled(ExecutionBundle.message("environment.variables.component.title"), myEnvVariablesComponent.getComponent());

    addAfter(builder);

    myLayout = builder.build();
  }

  protected void addBefore(@Nonnull FormBuilder builder) {
    // nothing
  }

  protected void addAfter(@Nonnull FormBuilder builder) {
    // nothing
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  public Component getComponent() {
    return myLayout;
  }

  @Nullable
  protected Project getProject() {
    return myModuleContext != null ? myModuleContext.getProject() : null;
  }

  @RequiredUIAccess
  private void showMacroDialog() {
    MacrosDialog dialog = new MacrosDialog(getProject(), myModuleContext);

    dialog.showAsync().doWhenDone(() -> {
      Macro selectedMacro = dialog.getSelectedMacro();
      if (selectedMacro != null) {
        myWorkDirectoryBox.setValue(selectedMacro.getDecoratedName());
      }
    });
  }

  public void setProgramParameters(String params) {
    myProgramParametersComponent.setValue(params);
  }

  public void addWorkingDirectoryListener(ValueComponent.ValueListener<String> onTextChange) {
    myWorkDirectoryBox.getComponent().addValueListener(onTextChange);
  }

  public void setWorkingDirectory(String dir) {
    myWorkDirectoryBox.setValue(dir);
  }

  public String getWorkingDirectory() {
    return myWorkDirectoryBox.getValue();
  }

  public void setModuleContext(Module moduleContext) {
    myModuleContext = moduleContext;
  }

  public void setHasModuleMacro() {
    myHasModuleMacro = true;
  }

  public void apply(P configuration) {
    configuration.setProgramParameters(myProgramParametersComponent.getValueOrError());
    configuration.setWorkingDirectory(myWorkDirectoryBox.getValue());

    configuration.setEnvs(myEnvVariablesComponent.getEnvs());
    configuration.setPassParentEnvs(myEnvVariablesComponent.isPassParentEnvs());
  }

  public void reset(P configuration) {
    setProgramParameters(configuration.getProgramParameters());
    setWorkingDirectory(PathUtil.toSystemDependentName(configuration.getWorkingDirectory()));

    myEnvVariablesComponent.setEnvs(configuration.getEnvs());
    myEnvVariablesComponent.setPassParentEnvs(configuration.isPassParentEnvs());
  }
}
