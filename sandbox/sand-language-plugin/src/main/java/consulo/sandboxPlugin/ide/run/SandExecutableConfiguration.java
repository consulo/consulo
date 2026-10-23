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

import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.PlatformAwareRunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.PlatformTargetFragmentState;
import consulo.execution.configuration.fragment.ProgramParametersFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.WorkingDirectoryFragmentState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class SandExecutableConfiguration extends LocatableConfigurationBase
    implements CommonProgramRunConfigurationParameters, PlatformAwareRunProfile {
    private static final String DEFAULT_PARAMETERS = "Hello, World!";

    public SandExecutableConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
        setProgramParameters(DEFAULT_PARAMETERS);
    }

    @Override
    protected Class<SandExecutableConfigurationOptions> getOptionsClass() {
        return SandExecutableConfigurationOptions.class;
    }

    @Override
    public SandExecutableConfigurationOptions getOptions() {
        return (SandExecutableConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        SandExecutableConfigurationLegacyState old = converter.readXml(SandExecutableConfigurationLegacyState.class);

        SandExecutableConfigurationOptions options = new SandExecutableConfigurationOptions();
        options.setExecutable(old.executable != null ? old.executable : SandExecutableConfigurationOptions.DEFAULT_EXECUTABLE);
        converter.setOptions(options);
        converter.addFragment(new PlatformTargetFragmentState(old.platformTarget));

        converter.addFragment(new ProgramParametersFragmentState(old.programParameters));
        converter.addFragment(new WorkingDirectoryFragmentState(old.workingDirectory));
        converter.addFragment(new EnvironmentFragmentState(converter.readEnvs(), !"false".equals(old.passParentEnvs)));
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(new SandExecutableFragment());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (StringUtil.isEmptyOrSpaces(getExecutable())) {
            throw new RuntimeConfigurationException(LocalizeValue.localizeTODO("Executable is not specified"));
        }
    }

    @Override
    public RunProfileState getState(Executor executor, ExecutionEnvironment env) throws ExecutionException {
        return new SandExecutableRunState(env, this);
    }

    public String getExecutable() {
        return getOptions().getExecutable();
    }

    public void setExecutable(String executable) {
        getOptions().setExecutable(executable);
    }
}
