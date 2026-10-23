/*
 * Copyright 2013-2016 consulo.io
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
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.ProgramParametersFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.WorkingDirectoryFragmentState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorGroup;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.profiler.SandProfilerConfigurationState;
import consulo.sandboxPlugin.ide.profiler.SandProfilerRunState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class SandConfiguration extends LocatableConfigurationBase implements CommonProgramRunConfigurationParameters {
    public SandConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    protected Class<SandConfigurationOptions> getOptionsClass() {
        return SandConfigurationOptions.class;
    }

    @Override
    public SandConfigurationOptions getOptions() {
        return (SandConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        SandConfigurationLegacyState old = converter.readXml(SandConfigurationLegacyState.class);

        SandConfigurationOptions options = new SandConfigurationOptions();
        options.setEntryPoint(old.entryPoint);
        options.setExpression(old.expression);
        options.setProfile(old.profile);
        options.setTags(old.tags);
        converter.setOptions(options);

        converter.addFragment(new ProgramParametersFragmentState(old.programParameters));
        converter.addFragment(new WorkingDirectoryFragmentState(old.workingDirectory));
        converter.addFragment(new EnvironmentFragmentState(converter.readEnvs(), !"false".equals(old.passParentEnvs)));
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(new SandEntryPointFragment(), new SandProfileFragment(), new SandTagsFragment());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
    }

    @Override
    public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment env) throws ExecutionException {
        if (ExecutorGroup.getGroupIfProxy(executor) instanceof SandExecutorGroup executorGroup) {
            SandExecutorSettings settings = executorGroup.getRegisteredSettings(executor.getId());
            if (settings != null) {
                return new SandProfileState(env, settings);
            }
        }

        SandProfilerConfigurationState profilerConfiguration = SandProfilerRunState.findConfiguration(executor);
        if (profilerConfiguration != null) {
            return new SandProfilerRunState(env, profilerConfiguration);
        }

        String executorId = executor.getId();
        if (DefaultRunExecutor.EXECUTOR_ID.equals(executorId) || DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)) {
            return new SandRunState(env);
        }
        return null;
    }

    public void setEntryPoint(@Nullable String entryPoint) {
        getOptions().setEntryPoint(entryPoint);
    }

    public @Nullable String getEntryPoint() {
        return getOptions().getEntryPoint();
    }

    public void setExpression(@Nullable String expression) {
        getOptions().setExpression(expression);
    }

    public @Nullable String getExpression() {
        return getOptions().getExpression();
    }

    public void setProfile(@Nullable String profile) {
        getOptions().setProfile(profile);
    }

    public @Nullable String getProfile() {
        return getOptions().getProfile();
    }

    public void setTags(@Nullable String tags) {
        getOptions().setTags(tags);
    }

    public @Nullable String getTags() {
        return getOptions().getTags();
    }
}
