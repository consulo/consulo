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
package consulo.execution.test.sm.runner.history;

import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.test.TestFrameworkRunningModel;
import consulo.execution.test.action.AbstractRerunFailedTestsAction;
import consulo.execution.test.sm.SMTestRunnerConnectionUtil;
import consulo.execution.test.sm.action.AbstractImportTestsAction;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.test.ui.BaseTestsOutputConsoleView;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.io.File;
import java.io.OutputStream;

public class ImportedTestRunnableState implements RunProfileState {
  private AbstractImportTestsAction.ImportRunProfile myRunProfile;
  private File myFile;

  public ImportedTestRunnableState(AbstractImportTestsAction.ImportRunProfile profile, File file) {
    myRunProfile = profile;
    myFile = file;
  }

  @jakarta.annotation.Nullable
  @Override
  public ExecutionResult execute(Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
    final MyEmptyProcessHandler handler = new MyEmptyProcessHandler();
    final SMTRunnerConsoleProperties properties = myRunProfile.getProperties();
    RunProfile configuration;
    final String frameworkName;
    if (properties != null) {
      configuration = properties.getConfiguration();
      frameworkName = properties.getTestFrameworkName();
    }
    else {
      configuration = myRunProfile;
      frameworkName = "Import Test Results";
    }
    final ImportedTestConsoleProperties consoleProperties = new ImportedTestConsoleProperties(properties, myFile, handler, myRunProfile.getProject(),
                                                                                              configuration, frameworkName, executor);
    final BaseTestsOutputConsoleView console = SMTestRunnerConnectionUtil.createConsole(consoleProperties.getTestFrameworkName(),
                                                                                        consoleProperties);
    final JComponent component = console.getComponent();
    AbstractRerunFailedTestsAction rerunFailedTestsAction = null;
    if (component instanceof TestFrameworkRunningModel) {
      rerunFailedTestsAction = consoleProperties.createRerunFailedTestsAction(console);
      if (rerunFailedTestsAction != null) {
        rerunFailedTestsAction.setModelProvider(() -> (TestFrameworkRunningModel)component);
      }
    }

    console.attachToProcess(handler);
    final DefaultExecutionResult result = new DefaultExecutionResult(console, handler);
    if (rerunFailedTestsAction != null) {
      result.setRestartActions(rerunFailedTestsAction);
    }
    return result;
  }

  private static class MyEmptyProcessHandler extends ProcessHandler {
    @Override
    protected void destroyProcessImpl() {}

    @Override
    protected void detachProcessImpl() {
      notifyProcessTerminated(0);
    }

    @Override
    public boolean detachIsDefault() {
      return false;
    }

    @jakarta.annotation.Nullable
    @Override
    public OutputStream getProcessInput() {
      return null;
    }
  }
}
