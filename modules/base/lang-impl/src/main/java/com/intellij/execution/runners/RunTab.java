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
package com.intellij.execution.runners;

import com.intellij.diagnostic.logging.LogConsoleManagerBase;
import com.intellij.diagnostic.logging.LogFilesManager;
import com.intellij.diagnostic.logging.OutputFileUtil;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.SearchScopeProvider;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.icons.AllIcons;
import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.psi.search.GlobalSearchScope;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class RunTab implements DataProvider, Disposable {
  @Nonnull
  protected final RunnerLayoutUi myUi;
  private LogFilesManager myManager;
  protected RunContentDescriptor myRunContentDescriptor;

  protected ExecutionEnvironment myEnvironment;
  protected final Project myProject;
  private final GlobalSearchScope mySearchScope;

  private LogConsoleManagerBase logConsoleManager;

  protected RunTab(@Nonnull ExecutionEnvironment environment, @Nonnull String runnerType) {
    this(environment.getProject(),
         SearchScopeProvider.createSearchScope(environment.getProject(), environment.getRunProfile()),
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

  protected RunTab(@Nonnull Project project, @Nonnull GlobalSearchScope searchScope, @Nonnull String runnerType, @Nonnull String runnerTitle, @Nonnull String sessionName) {
    myProject = project;
    mySearchScope = searchScope;

    myUi = RunnerLayoutUi.Factory.getInstance(project).create(runnerType, runnerTitle, sessionName, this);
    myUi.getContentManager().addDataProvider(this);
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (LangDataKeys.RUN_PROFILE == dataId) {
      return myEnvironment == null ? null : myEnvironment.getRunProfile();
    }
    else if (LangDataKeys.EXECUTION_ENVIRONMENT == dataId) {
      return myEnvironment;
    }
    else if (LangDataKeys.RUN_CONTENT_DESCRIPTOR == dataId) {
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
