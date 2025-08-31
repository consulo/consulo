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
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.application.ApplicationManager;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class CompoundRunConfiguration extends RunConfigurationBase implements WithoutOwnBeforeRunSteps, Cloneable {
  static final Comparator<RunConfiguration> COMPARATOR = (o1, o2) -> {
    int i = o1.getType().getDisplayName().compareTo(o2.getType().getDisplayName());
    return (i != 0) ? i : o1.getName().compareTo(o2.getName());
  };
  private Set<Pair<String, String>> myPairs = new HashSet<>();
  private Set<RunConfiguration> mySetToRun = new TreeSet<>(COMPARATOR);
  private boolean myInitialized = false;

  public CompoundRunConfiguration(Project project, @Nonnull CompoundRunConfigurationType type, String name) {
    super(project, type.getConfigurationFactories()[0], name);
  }

  public Set<RunConfiguration> getSetToRun() {
    initIfNeed();
    return mySetToRun;
  }

  private void initIfNeed() {
    if (myInitialized) return;
    mySetToRun.clear();
    RunManagerImpl manager = RunManagerImpl.getInstanceImpl(getProject());
    for (Pair<String, String> pair : myPairs) {
      RunnerAndConfigurationSettings settings = manager.findConfigurationByTypeAndName(pair.first, pair.second);
      if (settings != null && settings.getConfiguration() != this) {
        mySetToRun.add(settings.getConfiguration());
      }
    }
    myInitialized = true;
  }

  @Nonnull
  public Map<RunConfiguration, ExecutionTarget> getConfigurationsWithTargets(RunManager manager) {
    initIfNeed();
    Map<RunConfiguration, ExecutionTarget> map = new LinkedHashMap<>();
    // TODO [VISTAL] for now we not support configurations with executiontarget (idea like)
    getSetToRun().forEach(configuration -> map.put(configuration, null));
    return map;
  }

  @Nonnull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new CompoundRunConfigurationSettingsEditor(getProject());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (getSetToRun().isEmpty()) throw new RuntimeConfigurationException("There is nothing to run");
  }

  @Nullable
  @Override
  public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
    try {
      checkConfiguration();
    }
    catch (RuntimeConfigurationException e) {
      throw new ExecutionException(e.getMessage());
    }
    return new RunProfileState() {
      @Nullable
      @Override
      public ExecutionResult execute(Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
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

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);
    myPairs.clear();
    List<Element> children = element.getChildren("toRun");
    for (Element child : children) {
      String type = child.getAttributeValue("type");
      String name = child.getAttributeValue("name");
      if (type != null && name != null) {
        myPairs.add(Pair.create(type, name));
      }
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    for (RunConfiguration configuration : getSetToRun()) {
      Element child = new Element("toRun");
      child.setAttribute("type", configuration.getType().getId());
      child.setAttribute("name", configuration.getName());
      element.addContent(child);
    }
  }

  @Override
  public RunConfiguration clone() {
    CompoundRunConfiguration clone = (CompoundRunConfiguration)super.clone();
    clone.myPairs = new HashSet<>();
    clone.myPairs.addAll(myPairs);
    clone.mySetToRun = new TreeSet<>(COMPARATOR);
    clone.mySetToRun.addAll(getSetToRun());
    return clone;
  }
}
