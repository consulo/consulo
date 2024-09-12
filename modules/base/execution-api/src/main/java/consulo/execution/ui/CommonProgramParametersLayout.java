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

import consulo.application.AllIcons;
import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.awt.CommonProgramParametersPanel;
import consulo.execution.ui.awt.EnvironmentVariablesTextFieldWithBrowseButton;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.pathMacro.MacroSelector;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.FileChooserTextBoxBuilder;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import consulo.util.io.PathUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25/12/2021
 * <p>
 * Unified implementation of {@link CommonProgramParametersPanel}
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
        builder.addLabeled(ExecutionLocalize.runConfigurationProgramParameters().get(), myProgramParametersComponent);

        FileChooserTextBoxBuilder workDirBuilder = FileChooserTextBoxBuilder.create(getProject());
        workDirBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());
        workDirBuilder.dialogTitle(ExecutionLocalize.selectWorkingDirectoryMessage());
        workDirBuilder.dialogDescription(LocalizeValue.of());

        myWorkDirectoryBox = workDirBuilder.build();
        myWorkDirectoryBox.getComponent()
            .addFirstExtension(new TextBoxWithExtensions.Extension(false, PlatformIconGroup.generalInlinevariables(), PlatformIconGroup.generalInlinevariableshover(), event -> showMacroDialog()));

        builder.addLabeled(ExecutionLocalize.runConfigurationWorkingDirectoryLabel(), myWorkDirectoryBox.getComponent());

        myEnvVariablesComponent = new EnvironmentVariablesTextFieldWithBrowseButton();
        builder.addLabeled(ExecutionLocalize.environmentVariablesComponentTitle(), myEnvVariablesComponent.getComponent());

        addAfter(builder);

        myLayout = builder.build();
    }

    @RequiredUIAccess
    protected void addBefore(@Nonnull FormBuilder builder) {
        // nothing
    }

    @RequiredUIAccess
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
        MacroSelector.getInstance().select(getProject(), myModuleContext, macro -> {
            myWorkDirectoryBox.setValue(macro.getDecoratedName());
        });
    }

    @RequiredUIAccess
    public void setProgramParameters(String params) {
        myProgramParametersComponent.setValue(params);
    }

    @RequiredUIAccess
    public void addWorkingDirectoryListener(ComponentEventListener<ValueComponent<String>, ValueComponentEvent<String>> onTextChange) {
        myWorkDirectoryBox.getComponent().addValueListener(onTextChange);
    }

    @RequiredUIAccess
    public void setWorkingDirectory(String dir) {
        myWorkDirectoryBox.setValue(dir);
    }

    @RequiredUIAccess
    public String getWorkingDirectory() {
        return myWorkDirectoryBox.getValue();
    }

    public void setModuleContext(Module moduleContext) {
        myModuleContext = moduleContext;
    }

    public void setHasModuleMacro() {
        myHasModuleMacro = true;
    }

    @RequiredUIAccess
    public void apply(P configuration) {
        configuration.setProgramParameters(myProgramParametersComponent.getValueOrError());
        configuration.setWorkingDirectory(myWorkDirectoryBox.getValue());

        configuration.setEnvs(myEnvVariablesComponent.getEnvs());
        configuration.setPassParentEnvs(myEnvVariablesComponent.isPassParentEnvs());
    }

    @RequiredUIAccess
    public void reset(P configuration) {
        setProgramParameters(configuration.getProgramParameters());
        setWorkingDirectory(PathUtil.toSystemDependentName(configuration.getWorkingDirectory()));

        myEnvVariablesComponent.setEnvs(configuration.getEnvs());
        myEnvVariablesComponent.setPassParentEnvs(configuration.isPassParentEnvs());
    }
}
