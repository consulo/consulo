/*
 * Copyright 2013-2023 consulo.io
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
package consulo.process.util;

import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.CapturingProcessHandler;

/**
 * @author VISTALL
 * @since 04/06/2023
 */
public class CapturingProcessUtil {
  public static ProcessOutput execAndGetOutput(GeneralCommandLine commandLine) throws ExecutionException {
    if (isRemote(commandLine)) {
      return new CapturingProcessRunner(ProcessHandlerBuilder.create(commandLine).build()).runProcess();
    }
    return new CapturingProcessHandler(commandLine).runProcess();
  }

  public static ProcessOutput execAndGetOutput(GeneralCommandLine commandLine,
                                               int timeoutInMilliseconds) throws ExecutionException {
    if (isRemote(commandLine)) {
      return new CapturingProcessRunner(ProcessHandlerBuilder.create(commandLine).build()).runProcess(timeoutInMilliseconds);
    }
    return new CapturingProcessHandler(commandLine).runProcess(timeoutInMilliseconds);
  }

  /**
   * A command line of another machine runs through the platform's own process handler, a local process cannot start it.
   */
  private static boolean isRemote(GeneralCommandLine commandLine) {
    return !commandLine.getPlatform().equals(Platform.current());
  }
}
