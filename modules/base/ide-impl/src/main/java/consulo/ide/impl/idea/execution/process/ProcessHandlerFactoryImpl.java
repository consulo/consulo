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
package consulo.ide.impl.idea.execution.process;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.execution.configurations.PtyCommandLine;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.ProcessConsoleType;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.OSProcessHandler;
import consulo.process.local.ProcessHandlerFactory;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

@Singleton
@ServiceImpl
public class ProcessHandlerFactoryImpl extends ProcessHandlerFactory {
  @Nonnull
  @Override
  public ProcessHandler createProcessHandler(@Nonnull GeneralCommandLine commandLine, @Nonnull ProcessConsoleType processConsoleType) throws ExecutionException {
    switch (processConsoleType) {
      case BUILTIN:
        return new OSProcessHandler(commandLine);
      case EXTERNAL_EMULATION:
        PtyCommandLine ptyCommandLine = new PtyCommandLine(commandLine);
        OSProcessHandler handler = new ColoredProcessHandler(ptyCommandLine);
        handler.setHasPty(true);
        return handler;
      case EXTERNAL:
        Platform.OperatingSystem os = Platform.current().os();
        if (!os.isWindows()) {
          throw new ExecutionException("Can't create process with EXTERNAL console at OS " + os.name());
        }

        return RunnerMediator.newInstance().createProcess(commandLine, true);
      default:
        throw new IllegalArgumentException("Unknown console type " + processConsoleType);
    }
  }

  @Override
  @Nonnull
  public OSProcessHandler createColoredProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return new ColoredProcessHandler(commandLine);
  }

  @Nonnull
  @Override
  public ProcessHandler createKillableProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return new KillableProcessHandler(commandLine);
  }

  @Nonnull
  @Override
  public ProcessHandler createKillableColoredProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return new KillableColoredProcessHandler(commandLine);
  }
}
