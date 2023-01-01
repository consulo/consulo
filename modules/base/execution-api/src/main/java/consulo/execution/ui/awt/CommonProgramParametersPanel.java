/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.ui.awt;

import consulo.annotation.DeprecationInfo;
import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.ExecutionBundle;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.pathMacro.MacroSelector;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.FileChooserTextBoxBuilder;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.io.PathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

@Deprecated
@DeprecationInfo("Use CommonProgramParametersLayout")
public class CommonProgramParametersPanel extends JPanel implements PanelWithAnchor {
  private LabeledComponent<RawCommandLineEditor> myProgramParametersComponent;
  private LabeledComponent<JComponent> myWorkingDirectoryComponent;
  private FileChooserTextBoxBuilder.Controller myWorkDirectoryBox;
  private EnvironmentVariablesComponent myEnvVariablesComponent;
  protected JComponent myAnchor;

  private Module myModuleContext = null;
  private boolean myHasModuleMacro;

  public CommonProgramParametersPanel() {
    this(true);
  }

  public CommonProgramParametersPanel(boolean init) {
    super();

    setLayout(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 0, 5, true, false));

    if (init) {
      init();
    }
  }

  protected void init() {
    initComponents();
    updateUI();
    setupAnchor();
  }

  protected void setupAnchor() {
    myAnchor = UIUtil.mergeComponentsWithAnchor(myProgramParametersComponent, myWorkingDirectoryComponent, myEnvVariablesComponent);
  }

  @Nullable
  protected Project getProject() {
    return myModuleContext != null ? myModuleContext.getProject() : null;
  }

  @RequiredUIAccess
  protected void initComponents() {
    myProgramParametersComponent = LabeledComponent.create(new RawCommandLineEditor(), ExecutionBundle.message("run.configuration.program.parameters"));

    FileChooserTextBoxBuilder workDirBuilder = FileChooserTextBoxBuilder.create(getProject());
    workDirBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());
    workDirBuilder.dialogTitle(ExecutionBundle.message("select.working.directory.message"));
    workDirBuilder.dialogDescription(LocalizeValue.of());

    myWorkDirectoryBox = workDirBuilder.build();
    myWorkDirectoryBox.getComponent()
            .addFirstExtension(new TextBoxWithExtensions.Extension(false, PlatformIconGroup.generalInlinevariables(), PlatformIconGroup.generalInlinevariableshover(), event -> showMacroDialog()));

    myWorkingDirectoryComponent = LabeledComponent.create((JComponent)TargetAWT.to(myWorkDirectoryBox.getComponent()), ExecutionBundle.message("run.configuration.working.directory.label"));
    myEnvVariablesComponent = new EnvironmentVariablesComponent();

    myEnvVariablesComponent.setLabelLocation(BorderLayout.WEST);
    myProgramParametersComponent.setLabelLocation(BorderLayout.WEST);
    myWorkingDirectoryComponent.setLabelLocation(BorderLayout.WEST);

    addComponents();

    copyDialogCaption(myProgramParametersComponent);
  }

  @RequiredUIAccess
  private void showMacroDialog() {
    MacroSelector.getInstance().select(getProject(), myModuleContext, macro -> {
      myWorkDirectoryBox.setValue(macro.getDecoratedName());
    });
  }

  protected void addComponents() {
    add(myProgramParametersComponent);
    add(myWorkingDirectoryComponent);
    add(myEnvVariablesComponent);
  }

  protected void copyDialogCaption(final LabeledComponent<RawCommandLineEditor> component) {
    final RawCommandLineEditor rawCommandLineEditor = component.getComponent();
    rawCommandLineEditor.setDialogCaption(component.getRawText());
    component.getLabel().setLabelFor(rawCommandLineEditor.getTextField());
  }

  public void setProgramParametersLabel(String textWithMnemonic) {
    myProgramParametersComponent.setText(textWithMnemonic);
    copyDialogCaption(myProgramParametersComponent);
  }

  public void setProgramParameters(String params) {
    myProgramParametersComponent.getComponent().setText(params);
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

  public LabeledComponent<RawCommandLineEditor> getProgramParametersComponent() {
    return myProgramParametersComponent;
  }

  @Override
  public JComponent getAnchor() {
    return myAnchor;
  }

  @Override
  public void setAnchor(JComponent anchor) {
    myAnchor = anchor;
    myProgramParametersComponent.setAnchor(anchor);
    myWorkingDirectoryComponent.setAnchor(anchor);
    myEnvVariablesComponent.setAnchor(anchor);
  }

  public void applyTo(CommonProgramRunConfigurationParameters configuration) {
    configuration.setProgramParameters(fromTextField(myProgramParametersComponent.getComponent(), configuration));
    configuration.setWorkingDirectory(myWorkDirectoryBox.getValue());

    configuration.setEnvs(myEnvVariablesComponent.getEnvs());
    configuration.setPassParentEnvs(myEnvVariablesComponent.isPassParentEnvs());
  }

  @Nullable
  protected String fromTextField(@Nonnull TextAccessor textAccessor, @Nonnull CommonProgramRunConfigurationParameters configuration) {
    return textAccessor.getText();
  }

  public void reset(CommonProgramRunConfigurationParameters configuration) {
    setProgramParameters(configuration.getProgramParameters());
    setWorkingDirectory(PathUtil.toSystemDependentName(configuration.getWorkingDirectory()));

    myEnvVariablesComponent.setEnvs(configuration.getEnvs());
    myEnvVariablesComponent.setPassParentEnvs(configuration.isPassParentEnvs());
  }
}
