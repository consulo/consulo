/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.test.autotest;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.dataContext.DataManager;
import consulo.execution.*;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.RunContentManager;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.ex.content.Content;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractAutoTestManager implements PersistentStateComponent<AbstractAutoTestManager.State> {
  protected static final String AUTO_TEST_MANAGER_DELAY = "auto.test.manager.delay";
  protected static final int AUTO_TEST_MANAGER_DELAY_DEFAULT = 3000;
  private static final Key<ProcessListener> ON_TERMINATION_RESTARTER_KEY = Key.create("auto.test.manager.on.termination.restarter");
  private final Project myProject;
  private final Set<RunProfile> myEnabledRunProfiles = new HashSet<>();
  protected int myDelayMillis;
  private AutoTestWatcher myWatcher;

  public AbstractAutoTestManager(@Nonnull Project project) {
    myProject = project;
    myDelayMillis = ProjectPropertiesComponent.getInstance(project).getInt(AUTO_TEST_MANAGER_DELAY, AUTO_TEST_MANAGER_DELAY_DEFAULT);
    myWatcher = createWatcher(project);
  }

  @Nullable
  private static ExecutionEnvironment getCurrentEnvironment(@Nonnull Content content) {
    JComponent component = content.getComponent();
    if (component == null) {
      return null;
    }
    return DataManager.getInstance().getDataContext(component).getData(ExecutionDataKeys.EXECUTION_ENVIRONMENT);
  }

  private static void clearRestarterListener(@Nonnull ProcessHandler processHandler) {
    ProcessListener restarterListener = ON_TERMINATION_RESTARTER_KEY.get(processHandler, null);
    if (restarterListener != null) {
      processHandler.removeProcessListener(restarterListener);
      ON_TERMINATION_RESTARTER_KEY.set(processHandler, null);
    }
  }

  private static void restart(@Nonnull RunContentDescriptor descriptor) {
    descriptor.setActivateToolWindowWhenAdded(false);
    descriptor.setReuseToolWindowActivation(true);
    ExecutionUtil.restart(descriptor);
  }

  public static void saveConfigurationState(State state, RunProfile profile) {
    RunConfiguration runConfiguration = ObjectUtil.tryCast(profile, RunConfiguration.class);
    if (runConfiguration != null) {
      RunConfigurationDescriptor descriptor = new RunConfigurationDescriptor();
      descriptor.myType = runConfiguration.getType().getId();
      descriptor.myName = runConfiguration.getName();
      state.myEnabledRunConfigurations.add(descriptor);
    }
  }

  public static List<RunConfiguration> loadConfigurations(State state, Project project) {
    List<RunConfiguration> configurations = ContainerUtil.newArrayList();
    RunManager runManager = RunManager.getInstance(project);
    List<RunConfigurationDescriptor> descriptors = Lists.notNullize(state.myEnabledRunConfigurations);
    for (RunConfigurationDescriptor descriptor : descriptors) {
      if (descriptor.myType != null && descriptor.myName != null) {
        RunnerAndConfigurationSettings settings = runManager.findConfigurationByTypeAndName(descriptor.myType,
                                                                                            descriptor.myName);
        RunConfiguration configuration = settings != null ? settings.getConfiguration() : null;
        if (configuration != null) {
          configurations.add(configuration);
        }
      }
    }
    return configurations;
  }

  @Nonnull
  protected abstract AutoTestWatcher createWatcher(Project project);

  public void setAutoTestEnabled(@Nonnull RunContentDescriptor descriptor, @Nonnull ExecutionEnvironment environment, boolean enabled) {
    Content content = descriptor.getAttachedContent();
    if (content != null) {
      if (enabled) {
        myEnabledRunProfiles.add(environment.getRunProfile());
        myWatcher.activate();
      }
      else {
        myEnabledRunProfiles.remove(environment.getRunProfile());
        if (!hasEnabledAutoTests()) {
          myWatcher.deactivate();
        }
        ProcessHandler processHandler = descriptor.getProcessHandler();
        if (processHandler != null) {
          clearRestarterListener(processHandler);
        }
      }
    }
  }

  private boolean hasEnabledAutoTests() {
    RunContentManager contentManager = ExecutionManager.getInstance(myProject).getContentManager();
    for (RunContentDescriptor descriptor : contentManager.getAllDescriptors()) {
      if (isAutoTestEnabledForDescriptor(descriptor)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAutoTestEnabled(@Nonnull RunContentDescriptor descriptor) {
    return isAutoTestEnabledForDescriptor(descriptor);
  }

  private boolean isAutoTestEnabledForDescriptor(@Nonnull RunContentDescriptor descriptor) {
    Content content = descriptor.getAttachedContent();
    if (content != null) {
      ExecutionEnvironment environment = getCurrentEnvironment(content);
      return environment != null && myEnabledRunProfiles.contains(environment.getRunProfile());
    }
    return false;
  }

  protected void restartAllAutoTests(int modificationStamp) {
    RunContentManager contentManager = ExecutionManager.getInstance(myProject).getContentManager();
    boolean active = false;
    for (RunContentDescriptor descriptor : contentManager.getAllDescriptors()) {
      if (isAutoTestEnabledForDescriptor(descriptor)) {
        restartAutoTest(descriptor, modificationStamp, myWatcher);
        active = true;
      }
    }
    if (!active) {
      myWatcher.deactivate();
    }
  }

  private void restartAutoTest(@Nonnull RunContentDescriptor descriptor,
                               int modificationStamp,
                               @Nonnull AutoTestWatcher documentWatcher) {
    ProcessHandler processHandler = descriptor.getProcessHandler();
    if (processHandler != null && !processHandler.isProcessTerminated()) {
      scheduleRestartOnTermination(descriptor, processHandler, modificationStamp, documentWatcher);
    }
    else {
      restart(descriptor);
    }
  }

  private void scheduleRestartOnTermination(@Nonnull final RunContentDescriptor descriptor,
                                            @Nonnull final ProcessHandler processHandler,
                                            final int modificationStamp,
                                            @Nonnull final AutoTestWatcher watcher) {
    ProcessListener restarterListener = ON_TERMINATION_RESTARTER_KEY.get(processHandler);
    if (restarterListener != null) {
      clearRestarterListener(processHandler);
    }
    restarterListener = new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        clearRestarterListener(processHandler);
        ApplicationManager.getApplication().invokeLater(() -> {
          if (isAutoTestEnabledForDescriptor(descriptor) && watcher.isUpToDate(modificationStamp)) {
            restart(descriptor);
          }
        }, Application.get().getAnyModalityState());
      }
    };
    ON_TERMINATION_RESTARTER_KEY.set(processHandler, restarterListener);
    processHandler.addProcessListener(restarterListener);
  }

  public int getDelay() {
    return myDelayMillis;
  }

  public void setDelay(int delay) {
    myDelayMillis = delay;
    myWatcher.deactivate();
    myWatcher = createWatcher(myProject);
    if (hasEnabledAutoTests()) {
      myWatcher.activate();
    }
    ProjectPropertiesComponent.getInstance(myProject).setValue(AUTO_TEST_MANAGER_DELAY, myDelayMillis, AUTO_TEST_MANAGER_DELAY_DEFAULT);
  }

  @Nullable
  @Override
  public State getState() {
    State state = new State();
    for (RunProfile profile : myEnabledRunProfiles) {
      saveConfigurationState(state, profile);
    }
    return state;
  }

  @Override
  public void loadState(State state) {
    List<RunConfiguration> configurations = loadConfigurations(state, myProject);
    myEnabledRunProfiles.clear();
    myEnabledRunProfiles.addAll(configurations);
    if (!configurations.isEmpty()) {
      myWatcher.activate();
    }
  }

  public static class State {
    @Tag("enabled-run-configurations")
    @AbstractCollection(surroundWithTag = false)
    List<AutoTestManager.RunConfigurationDescriptor> myEnabledRunConfigurations = ContainerUtil.newArrayList();
  }

  @Tag("run-configuration")
  static class RunConfigurationDescriptor {
    @Attribute("type")
    String myType;

    @Attribute("name")
    String myName;
  }
}
