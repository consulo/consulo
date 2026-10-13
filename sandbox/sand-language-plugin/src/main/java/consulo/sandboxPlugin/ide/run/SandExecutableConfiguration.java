/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.PlatformAwareRunProfile;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.CommonProgramParametersLayout;
import consulo.localize.LocalizeValue;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class SandExecutableConfiguration extends LocatableConfigurationBase
    implements CommonProgramRunConfigurationParameters, PlatformAwareRunProfile {
    private static final String DEFAULT_EXECUTABLE = "/bin/echo";
    private static final String DEFAULT_PARAMETERS = "Hello, World!";

    private String myExecutable = DEFAULT_EXECUTABLE;
    private @Nullable String myProgramParameters = DEFAULT_PARAMETERS;
    private @Nullable String myWorkingDirectory;
    private final Map<String, String> myEnvs = new LinkedHashMap<>();
    private boolean myPassParentEnvs = true;
    private @Nullable String myPlatformTargetId;

    public SandExecutableConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SandExecutableConfigurationEditor();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (StringUtil.isEmptyOrSpaces(myExecutable)) {
            throw new RuntimeConfigurationException(LocalizeValue.localizeTODO("Executable is not specified"));
        }
    }

    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment env) throws ExecutionException {
        return new SandExecutableRunState(env, this);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        myExecutable = element.getAttributeValue("executable", DEFAULT_EXECUTABLE);
        myProgramParameters = element.getAttributeValue("programParameters");
        myWorkingDirectory = element.getAttributeValue("workingDirectory");
        myPassParentEnvs = !"false".equals(element.getAttributeValue("passParentEnvs"));
        myPlatformTargetId = element.getAttributeValue("platformTarget");

        myEnvs.clear();
        for (Element env : element.getChildren("env")) {
            String name = env.getAttributeValue("name");
            if (name != null) {
                myEnvs.put(name, env.getAttributeValue("value", ""));
            }
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        element.setAttribute("executable", myExecutable);
        if (myProgramParameters != null) {
            element.setAttribute("programParameters", myProgramParameters);
        }
        if (myWorkingDirectory != null) {
            element.setAttribute("workingDirectory", myWorkingDirectory);
        }
        element.setAttribute("passParentEnvs", String.valueOf(myPassParentEnvs));
        if (myPlatformTargetId != null) {
            element.setAttribute("platformTarget", myPlatformTargetId);
        }

        for (Map.Entry<String, String> entry : myEnvs.entrySet()) {
            Element env = new Element("env");
            env.setAttribute("name", entry.getKey());
            env.setAttribute("value", entry.getValue());
            element.addContent(env);
        }
    }

    @Override
    public @Nullable String getPlatformTargetId() {
        return myPlatformTargetId;
    }

    @Override
    public void setPlatformTargetId(@Nullable String targetId) {
        myPlatformTargetId = targetId;
    }

    public String getExecutable() {
        return myExecutable;
    }

    public void setExecutable(String executable) {
        myExecutable = executable.trim();
    }

    @Override
    public void setProgramParameters(@Nullable String value) {
        myProgramParameters = StringUtil.nullize(value);
    }

    @Override
    public @Nullable String getProgramParameters() {
        return myProgramParameters;
    }

    @Override
    public void setWorkingDirectory(@Nullable String value) {
        myWorkingDirectory = StringUtil.nullize(value);
    }

    @Override
    public @Nullable String getWorkingDirectory() {
        return myWorkingDirectory;
    }

    @Override
    public void setEnvs(Map<String, String> envs) {
        myEnvs.clear();
        myEnvs.putAll(envs);
    }

    @Override
    public Map<String, String> getEnvs() {
        return myEnvs;
    }

    @Override
    public void setPassParentEnvs(boolean passParentEnvs) {
        myPassParentEnvs = passParentEnvs;
    }

    @Override
    public boolean isPassParentEnvs() {
        return myPassParentEnvs;
    }

    private static class SandExecutableConfigurationEditor extends SettingsEditor<SandExecutableConfiguration> {
        private @Nullable CommonProgramParametersLayout<SandExecutableConfiguration> myLayout;
        private @Nullable TextBox myExecutableBox;

        @RequiredUIAccess
        @Override
        protected void resetEditorFrom(SandExecutableConfiguration configuration) {
            CommonProgramParametersLayout<SandExecutableConfiguration> layout = myLayout;
            if (layout != null) {
                layout.reset(configuration);
            }

            TextBox executableBox = myExecutableBox;
            if (executableBox != null) {
                executableBox.setValue(configuration.getExecutable());
            }
        }

        @RequiredUIAccess
        @Override
        protected void applyEditorTo(SandExecutableConfiguration configuration) throws ConfigurationException {
            CommonProgramParametersLayout<SandExecutableConfiguration> layout = myLayout;
            if (layout != null) {
                layout.apply(configuration);
            }

            TextBox executableBox = myExecutableBox;
            if (executableBox != null) {
                configuration.setExecutable(StringUtil.notNullize(executableBox.getValue()));
            }
        }

        @RequiredUIAccess
        @Override
        protected Component createUIComponent() {
            CommonProgramParametersLayout<SandExecutableConfiguration> layout =
                new CommonProgramParametersLayout<>(Application.get().getInstance(DialogService.class)) {
                    @RequiredUIAccess
                    @Override
                    protected void addBefore(FormBuilder builder) {
                        TextBox executableBox = TextBox.create();
                        builder.addLabeled(LocalizeValue.localizeTODO("Executable:"), executableBox);
                        myExecutableBox = executableBox;
                    }
                };
            layout.build();
            myLayout = layout;
            return layout.getComponent();
        }
    }
}
