/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.process;

import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import jakarta.annotation.Nonnull;

public class RunnerWinProcess extends ProcessWrapper {

  private RunnerWinProcess(@Nonnull Process originalProcess) {
    super(originalProcess);
  }

  /**
   * Sends Ctrl+C or Ctrl+Break event to the process.
   * @param softKill if true, Ctrl+C event will be sent (otherwise, Ctrl+Break)
   */
  public void destroyGracefully(boolean softKill) {
    RunnerMediator.destroyProcess(this, softKill);
  }

  @Nonnull
  public static RunnerWinProcess create(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    if (!Platform.current().os().isWindows()) {
      throw new RuntimeException(RunnerWinProcess.class.getSimpleName() + " works on Windows only!");
    }
    RunnerMediator.injectRunnerCommand(commandLine);
    Process process = commandLine.createProcess();
    return new RunnerWinProcess(process);
  }
}
