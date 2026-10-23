/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.impl.internal.compound;

import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.RunnerAndConfigurationSettingsImpl;
import consulo.execution.*;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.application.ApplicationManager;
import consulo.process.ExecutionException;
import consulo.project.Project;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class CompoundRunConfiguration extends RunConfigurationBase implements WithoutOwnBeforeRunSteps, Cloneable {
  static final Comparator<RunConfiguration> COMPARATOR = (o1, o2) -> {
    int i = o1.getType().getDisplayName().compareTo(o2.getType().getDisplayName());
    return (i != 0) ? i : o1.getName().compareTo(o2.getName());
  };

  public CompoundRunConfiguration(Project project, CompoundRunConfigurationType type, String name) {
    super(project, type.getConfigurationFactories()[0], name);
  }

  @Override
  protected Class<CompoundRunConfigurationOptions> getOptionsClass() {
    return CompoundRunConfigurationOptions.class;
  }

  @Override
  public CompoundRunConfigurationOptions getOptions() {
    return (CompoundRunConfigurationOptions)super.getOptions();
  }

  @Override
  protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
    CompoundRunConfigurationOptions options = new CompoundRunConfigurationOptions();
    List<CompoundRunConfigurationTarget> targets = new ArrayList<>();
    for (Element child : converter.getElement().getChildren("toRun")) {
      String type = child.getAttributeValue("type");
      String name = child.getAttributeValue("name");
      if (type != null && name != null) {
        targets.add(new CompoundRunConfigurationTarget(type, name));
      }
    }
    options.setTargets(targets);
    converter.setOptions(options);
  }

  @Override
  public List<RunConfigurationFragment<?>> createFragments() {
    return List.of(new CompoundRunConfigurationFragment());
  }

  /**
   * The configurations to run, resolved by type and name; missing ones are left out.
   */
  public Set<RunConfiguration> getSetToRun() {
    Set<RunConfiguration> result = new TreeSet<>(COMPARATOR);
    RunManagerImpl manager = RunManagerImpl.getInstanceImpl(getProject());
    for (CompoundRunConfigurationTarget target : getOptions().getTargets()) {
      String type = target.getType();
      String name = target.getName();
      if (type == null || name == null) {
        continue;
      }
      RunnerAndConfigurationSettings settings = manager.findConfigurationByTypeAndName(type, name);
      if (settings != null && settings.getConfiguration() != this) {
        result.add(settings.getConfiguration());
      }
    }
    return result;
  }

  public void setSetToRun(Collection<RunConfiguration> configurations) {
    List<RunConfiguration> sorted = new ArrayList<>(configurations);
    sorted.sort(COMPARATOR);
    List<CompoundRunConfigurationTarget> targets = new ArrayList<>();
    for (RunConfiguration configuration : sorted) {
      targets.add(new CompoundRunConfigurationTarget(configuration.getType().getId(), configuration.getName()));
    }
    getOptions().setTargets(targets);
  }

  public Map<RunConfiguration, ExecutionTarget> getConfigurationsWithTargets(RunManager manager) {
    Map<RunConfiguration, ExecutionTarget> map = new LinkedHashMap<>();
    // TODO [VISTAL] for now we not support configurations with executiontarget (idea like)
    getSetToRun().forEach(configuration -> map.put(configuration, null));
    return map;
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (getSetToRun().isEmpty()) throw new RuntimeConfigurationException("There is nothing to run");
  }

  @Override
  public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
    try {
      checkConfiguration();
    }
    catch (RuntimeConfigurationException e) {
      throw new ExecutionException(e.getMessage());
    }
    return new RunProfileState() {
      @Override
      public @Nullable ExecutionResult execute(Executor executor, ProgramRunner runner) throws ExecutionException {
        ApplicationManager.getApplication().invokeLater(() -> {
          RunManagerImpl manager = RunManagerImpl.getInstanceImpl(getProject());
          for (RunConfiguration configuration : getSetToRun()) {
            RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(manager, configuration, false);
            ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(executor, settings);
            if (builder != null) {
              ExecutionManager.getInstance(getProject())
                      .restartRunProfile(builder.activeTarget().dataContext(null).build());
            }
          }
        });
        return null;
      }
    };
  }
}
