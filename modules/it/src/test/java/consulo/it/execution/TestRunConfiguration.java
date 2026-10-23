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
package consulo.it.execution;

import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.ProgramParametersFragmentState;
import consulo.execution.configuration.fragment.WorkingDirectoryFragmentState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

public class TestRunConfiguration extends LocatableConfigurationBase implements CommonProgramRunConfigurationParameters {
    public TestRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    protected Class<TestRunConfigurationOptions> getOptionsClass() {
        return TestRunConfigurationOptions.class;
    }

    @Override
    public TestRunConfigurationOptions getOptions() {
        return (TestRunConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        TestRunConfigurationLegacyState old = converter.readXml(TestRunConfigurationLegacyState.class);

        TestRunConfigurationOptions options = new TestRunConfigurationOptions();
        options.setMainClass(old.MAIN_CLASS_NAME);
        options.setVerbose(old.VERBOSE);
        converter.setOptions(options);

        converter.addFragment(new ProgramParametersFragmentState(old.PROGRAM_PARAMETERS));
        converter.addFragment(new WorkingDirectoryFragmentState(old.WORKING_DIRECTORY));
        converter.addFragment(new EnvironmentFragmentState(converter.readEnvs(), old.PASS_PARENT_ENVS));
    }

    @Override
    public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment environment) {
        return null;
    }
}
