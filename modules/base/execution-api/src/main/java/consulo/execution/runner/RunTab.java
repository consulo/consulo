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
package consulo.execution.runner;

import consulo.application.AllIcons;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.execution.ExecutionDataKeys;
import consulo.execution.configuration.ExecutionSearchScopeProvider;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.log.LogConsoleManagerBase;
import consulo.execution.configuration.log.LogFilesManager;
import consulo.execution.configuration.log.OutputFileUtil;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.execution.ui.layout.RunnerLayoutUiFactory;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class RunTab implements DataProvider, Disposable {
  @Nonnull
  protected final RunnerLayoutUi myUi;
  private LogFilesManager myManager;
  protected RunContentDescriptor myRunContentDescriptor;

  protected ExecutionEnvironment myEnvironment;
  protected final Project myProject;
  private final SearchScope mySearchScope;

  private LogConsoleManagerBase logConsoleManager;

  protected RunTab(@Nonnull ExecutionEnvironment environment, @Nonnull String runnerType) {
    this(environment.getProject(),
         ExecutionSearchScopeProvider.createSearchScope(environment.getProject(), environment.getRunProfile()),
         runnerType,
         environment.getExecutor().getId(),
         environment.getRunProfile().getName());

    myEnvironment = environment;
  }

  @Override
  public void dispose() {
    myRunContentDescriptor = null;
    myEnvironment = null;
    logConsoleManager = null;
  }

  protected RunTab(@Nonnull Project project, @Nonnull SearchScope searchScope, @Nonnull String runnerType, @Nonnull String runnerTitle, @Nonnull String sessionName) {
    myProject = project;
    mySearchScope = searchScope;

    myUi = RunnerLayoutUiFactory.getInstance(project).create(runnerType, runnerTitle, sessionName, this);
    myUi.getContentManager().addDataProvider(this);
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (ExecutionDataKeys.RUN_PROFILE == dataId) {
      return myEnvironment == null ? null : myEnvironment.getRunProfile();
    }
    else if (ExecutionDataKeys.EXECUTION_ENVIRONMENT == dataId) {
      return myEnvironment;
    }
    else if (ExecutionDataKeys.RUN_CONTENT_DESCRIPTOR == dataId) {
      return myRunContentDescriptor;
    }
    return null;
  }

  @Nonnull
  public LogConsoleManagerBase getLogConsoleManager() {
    if (logConsoleManager == null) {
      logConsoleManager = new LogConsoleManagerBase(myProject, mySearchScope) {
        @Override
        protected Image getDefaultIcon() {
          return AllIcons.Debugger.Console;
        }

        @Override
        protected RunnerLayoutUi getUi() {
          return myUi;
        }

        @Override
        public ProcessHandler getProcessHandler() {
          return myRunContentDescriptor == null ? null : myRunContentDescriptor.getProcessHandler();
        }
      };
    }
    return logConsoleManager;
  }

  protected final void initLogConsoles(@Nonnull RunProfile runConfiguration, @Nonnull RunContentDescriptor contentDescriptor, @Nullable ExecutionConsole console) {
    ProcessHandler processHandler = contentDescriptor.getProcessHandler();
    if (runConfiguration instanceof RunConfigurationBase) {
      RunConfigurationBase configuration = (RunConfigurationBase)runConfiguration;
      if (myManager == null) {
        myManager = new LogFilesManager(getLogConsoleManager());
      }
      myManager.addLogConsoles(configuration, processHandler);
      if (processHandler != null) {
        OutputFileUtil.attachDumpListener(configuration, processHandler, console);
      }
    }
  }
}
