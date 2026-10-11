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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.compiler.CompilerConfiguration;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.internal.ProjectEx;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * @author Eugene Zhuravlev
 * @since 2003-12-15
 */
@ExtensionImpl
public class ProjectConfigurableGroup
    implements ProjectConfigurable, ConfigurableWeight, NonDefaultProjectConfigurable, Configurable.NoScroll {
    private final Project myProject;

    private @Nullable Component myComponent;
    private @Nullable TextBox myProjectName;
    private FileChooserTextBoxBuilder.@Nullable Controller myCompilerPathController;

    private boolean myFreeze = false;

    @Inject
    public ProjectConfigurableGroup(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return StandardConfigurableIds.PROJECT_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.projectRootsProjectDisplayName();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        TextBox projectName = TextBox.create();
        myProjectName = projectName;

        FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(myProject);
        builder.uiDisposable(parentUIDisposable);
        builder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());

        FileChooserTextBoxBuilder.Controller compilerPathController = builder.build();
        compilerPathController.getComponent().addValueListener(event -> {
            if (!myFreeze) {
                getModulesConfigurator().processModuleCompilerOutputChanged(getCompilerOutputUrl());
            }
        });
        myCompilerPathController = compilerPathController;

        FormBuilder formBuilder = FormBuilder.create();
        formBuilder.addLabeled(ProjectLocalize.projectNameLabel(), projectName);
        formBuilder.addLabeled(ProjectLocalize.projectCompilerOutputLabel(), compilerPathController.getComponent());
        formBuilder.addBottom(HtmlLabel.create(ProjectLocalize.projectCompilerOutputDescription()));

        component = formBuilder.build();
        myComponent = component;
        return component;
    }

    private ModulesConfigurator getModulesConfigurator() {
        ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil) ShowSettingsUtil.getInstance();
        return util.getModulesModel(myProject);
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myComponent = null;
        myProjectName = null;
        myCompilerPathController = null;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        myFreeze = true;
        try {
            FileChooserTextBoxBuilder.Controller compilerPathController = myCompilerPathController;
            String compilerOutput = getModulesConfigurator().getCompilerOutputUrl();
            if (compilerPathController != null && compilerOutput != null) {
                compilerPathController.setValue(FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(compilerOutput)));
            }

            TextBox projectName = myProjectName;
            if (projectName != null) {
                projectName.setValue(myProject.getName(), false);
            }
        }
        finally {
            myFreeze = false;
        }
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        TextBox projectName = myProjectName;
        FileChooserTextBoxBuilder.Controller compilerPathController = myCompilerPathController;
        if (projectName == null || compilerPathController == null) {
            return;
        }

        if (StringUtil.isEmptyOrSpaces(projectName.getValue())) {
            throw new ConfigurationException(ProjectLocalize.projectNameRequiredError());
        }

        getModulesConfigurator().setCompilerOutputUrl(getCompilerOutputUrl());

        CompilerConfiguration configuration = CompilerConfiguration.getInstance(myProject);
        Application.get().runWriteAction(() -> {
            String canonicalPath = compilerPathController.getValue();
            if (canonicalPath != null && canonicalPath.length() > 0) {
                try {
                    canonicalPath = FileUtil.resolveShortWindowsName(canonicalPath);
                }
                catch (IOException ignored) {
                }
                canonicalPath = FileUtil.toSystemIndependentName(canonicalPath);
                configuration.setCompilerOutputUrl(VirtualFileUtil.pathToUrl(canonicalPath));
            }
            else {
                configuration.setCompilerOutputUrl(null);
            }

            ((ProjectEx) myProject).setProjectName(StringUtil.notNullize(projectName.getValue()));
        });
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        TextBox projectName = myProjectName;
        FileChooserTextBoxBuilder.Controller compilerPathController = myCompilerPathController;
        if (projectName == null || compilerPathController == null) {
            return false;
        }

        String compilerOutput = CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();
        if (!Comparing.strEqual(
            FileUtil.toSystemIndependentName(VirtualFileUtil.urlToPath(compilerOutput)),
            FileUtil.toSystemIndependentName(compilerPathController.getValue())
        )) {
            return true;
        }

        return !StringUtil.notNullize(projectName.getValue()).trim().equals(myProject.getName());
    }

    public String getCompilerOutputUrl() {
        FileChooserTextBoxBuilder.Controller compilerPathController = myCompilerPathController;
        return compilerPathController == null ? "" : VirtualFileUtil.pathToUrl(compilerPathController.getValue().trim());
    }

    @Override
    public int getConfigurableWeight() {
        return Integer.MAX_VALUE - 1;
    }
}
