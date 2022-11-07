/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.execution.process.impl;

import consulo.ide.impl.idea.execution.configurations.PtyCommandLine;
import consulo.ide.impl.idea.execution.process.ColoredProcessHandler;
import consulo.ide.impl.idea.execution.process.KillableColoredProcessHandler;
import consulo.ide.impl.idea.execution.process.KillableProcessHandler;
import consulo.ide.impl.idea.execution.process.RunnerMediator;
import consulo.platform.Platform;
import consulo.process.*;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.OSProcessHandler;
import consulo.process.io.BaseOutputReader;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/11/2022
 */
public class ProcessHandlerBuilderImpl implements ProcessHandlerBuilder {
  private ProcessConsoleType myConsoleType = ProcessConsoleType.BUILTIN;
  private boolean myColored = false;
  private boolean myKillable = false;
  private boolean mySilentReader = false;

  private final GeneralCommandLine myCommandLine;

  public ProcessHandlerBuilderImpl(@Nonnull GeneralCommandLine commandLine) {
    myCommandLine = commandLine;
  }

  @Nonnull
  @Override
  public ProcessHandlerBuilder colored() {
    myColored = true;
    return this;
  }

  @Nonnull
  @Override
  public ProcessHandlerBuilder killable() {
    myKillable = true;
    return this;
  }

  @Nonnull
  @Override
  public ProcessHandlerBuilder silentReader() {
    mySilentReader = true;
    return this;
  }

  @Nonnull
  @Override
  public ProcessHandlerBuilder consoleType(@Nonnull ProcessConsoleType type) {
    myConsoleType = type;
    return this;
  }

  @Nonnull
  @Override
  public ProcessHandler build() throws ExecutionException {
    ProcessHandler processHandler = null;
    switch (myConsoleType) {
      case BUILTIN:
        processHandler = createLocalProcessHandler(myCommandLine);
        break;
      case EXTERNAL_EMULATION:
        PtyCommandLine ptyCommandLine = new PtyCommandLine(myCommandLine);
        ProcessHandler handler = createLocalProcessHandler(ptyCommandLine);
        if (handler instanceof OSProcessHandler osProcessHandler) {
          osProcessHandler.setHasPty(true);
        }
        processHandler = handler;
        break;
      case EXTERNAL:
        if (mySilentReader) {
          throw new IllegalArgumentException("Silent reader not support for EXTERNAL console");
        }
        
        Platform.OperatingSystem os = Platform.current().os();
        if (!os.isWindows()) {
          throw new ExecutionException("Can't create process with EXTERNAL console at OS " + os.name());
        }

        processHandler = RunnerMediator.newInstance().createProcess(myCommandLine, true);
        break;
      default:
        throw new IllegalArgumentException("Unknown console type " + myConsoleType);
    }

    if (myKillable && !(processHandler instanceof KillableProcess)) {
      throw new IllegalArgumentException("Process created killable but not instance of " + KillableProcess.class);
    }

    return processHandler;
  }

  private ProcessHandler createLocalProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    ProcessHandler processHandler = null;

    if (myKillable) {
      if (myColored) {
        processHandler = new KillableColoredProcessHandler(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            return mySilentReader ? BaseOutputReader.Options.forMostlySilentProcess() : super.readerOptions();
          }
        };
      }
      else {
        processHandler = new KillableProcessHandler(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            return mySilentReader ? BaseOutputReader.Options.forMostlySilentProcess() : super.readerOptions();
          }
        };
      }
    }
    else {
      if (myColored) {
        processHandler = new ColoredProcessHandler(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            return mySilentReader ? BaseOutputReader.Options.forMostlySilentProcess() : super.readerOptions();
          }
        };
      }
      else {
        processHandler = new OSProcessHandler(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            return mySilentReader ? BaseOutputReader.Options.forMostlySilentProcess() : super.readerOptions();
          }
        };
      }
    }

    return processHandler;
  }
}
