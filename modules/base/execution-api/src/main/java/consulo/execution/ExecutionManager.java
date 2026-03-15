/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.execution;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.RunContentManager;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Manages the execution of run configurations and the relationship between running processes and Run/Debug toolwindow tabs.
 */
@ServiceAPI(value = ComponentScope.PROJECT)
public interface ExecutionManager {
  public static ExecutionManager getInstance(Project project) {
    return project.getInstance(ExecutionManager.class);
  }

  /**
   * Returns the manager of running process tabs in Run and Debug toolwindows.
   *
   * @return the run content manager instance.
   */
  
  @Deprecated
  @DeprecationInfo("RunContentManager is project service, use constructor injection")
  RunContentManager getContentManager();

  /**
   * Returns the list of processes managed by all open run and debug tabs.
   *
   * @return the list of processes.
   */
  
  ProcessHandler[] getRunningProcesses();

  
  List<RunContentDescriptor> getRunningDescriptors(Predicate<? super RunnerAndConfigurationSettings> condition);

  
  List<RunContentDescriptor> getDescriptors(Predicate<? super RunnerAndConfigurationSettings> condition);

  /**
   * Prepares the run or debug tab for running the specified process and calls a callback to start it.
   *
   * @param starter     the callback to start the process execution.
   * @param state       the ready-to-start process
   * @param environment the execution environment describing the process to be started.
   */
  @RequiredUIAccess
  void startRunProfile(RunProfileStarter starter,
                       RunProfileState state,
                       ExecutionEnvironment environment);

  default void restartRunProfile(Project project,
                                 Executor executor,
                                 ExecutionTarget target,
                                 @Nullable RunnerAndConfigurationSettings configuration,
                                 @Nullable ProcessHandler processHandler) {
    restartRunProfile(project, executor, target, configuration, processHandler, executionEnvironment -> {
    });
  }

  void restartRunProfile(Project project,
                         Executor executor,
                         ExecutionTarget target,
                         @Nullable RunnerAndConfigurationSettings configuration,
                         @Nullable ProcessHandler processHandler,
                         Consumer<ExecutionEnvironment> envCustomizer);

  void restartRunProfile(ExecutionEnvironment environment);

  boolean isStarting(String exectutorId, String runnerId);
}
