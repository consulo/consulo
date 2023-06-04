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
import consulo.ide.impl.idea.execution.process.*;
import consulo.ide.impl.idea.execution.process.KillableColoredProcessHandlerImpl;
import consulo.ide.impl.idea.execution.process.KillableProcessHandlerImpl;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.process.*;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.OSProcessHandler;
import consulo.process.io.BaseOutputReader;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/11/2022
 */
public class ProcessHandlerBuilderImpl implements ProcessHandlerBuilder {
  private ProcessConsoleType myConsoleType = ProcessConsoleType.BUILTIN;
  private boolean myColored = false;
  private boolean myKillable = false;
  private boolean myBinary = false;
  private Boolean myShouldDestroyProcessRecursively;
  private Boolean myShouldKillProcessSoftly;

  private BaseOutputReader.Options myReaderOptions;

  private final GeneralCommandLine myCommandLine;

  public ProcessHandlerBuilderImpl(@Nonnull GeneralCommandLine commandLine) {
    myCommandLine = commandLine;
  }

  @Nonnull
  @Override
  public ProcessHandlerBuilder binary() {
    myBinary = true;
    return this;
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
    myReaderOptions = BaseOutputReader.Options.forMostlySilentProcess();
    return this;
  }

  @Nonnull
  @Override
  public ProcessHandlerBuilder blockingReader() {
    myReaderOptions = BaseOutputReader.Options.BLOCKING;
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
  public ProcessHandlerBuilder shouldDestroyProcessRecursively(boolean destoryRecursive) {
    myShouldDestroyProcessRecursively = destoryRecursive;
    return this;
  }

  @Nonnull
  @Override
  public ProcessHandlerBuilder shouldKillProcessSoftly(boolean killProcessSoftly) {
    myShouldKillProcessSoftly = killProcessSoftly;
    return this;
  }

  @Nonnull
  @Override
  public ProcessHandler build() throws ExecutionException {
    if (myBinary) {
      return new BinaryOSProcessHandlerImpl(myCommandLine);
    }
    
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
        if (myReaderOptions != null) {
          throw new IllegalArgumentException("Reader options not support for console");
        }

        PlatformOperatingSystem os = Platform.current().os();
        if (!os.isWindows()) {
          throw new ExecutionException("Can't create process with EXTERNAL console at OS " + os.name());
        }

        processHandler = RunnerMediator.newInstance().createProcess(myCommandLine, true);
        break;
      default:
        throw new IllegalArgumentException("Unknown console type " + myConsoleType);
    }

    if (myKillable && !(processHandler instanceof KillableProcessHandler)) {
      throw new IllegalArgumentException("Process created killable but not instance of " + KillableProcessHandler.class);
    }

    return processHandler;
  }

  private ProcessHandler createLocalProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    OSProcessHandler processHandler = null;

    if (myKillable) {
      if (myColored) {
        processHandler = new KillableColoredProcessHandlerImpl(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            if (myReaderOptions != null) {
              return myReaderOptions;
            }
            return super.readerOptions();
          }
        };
      }
      else {
        processHandler = new KillableProcessHandlerImpl(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            if (myReaderOptions != null) {
              return myReaderOptions;
            }
            return super.readerOptions();
          }
        };
      }
    }
    else {
      if (myColored) {
        processHandler = new ColoredProcessHandlerImpl(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            if (myReaderOptions != null) {
              return myReaderOptions;
            }
            return super.readerOptions();
          }
        };
      }
      else {
        processHandler = new OSProcessHandler(commandLine) {
          @Nonnull
          @Override
          protected BaseOutputReader.Options readerOptions() {
            if (myReaderOptions != null) {
              return myReaderOptions;
            }
            return super.readerOptions();
          }
        };
      }
    }

    if (myShouldDestroyProcessRecursively != null) {
      processHandler.setShouldDestroyProcessRecursively(myShouldDestroyProcessRecursively);
    }

    if (myShouldKillProcessSoftly != null && processHandler instanceof KillableProcessHandlerImpl killableProcessHandler) {
      killableProcessHandler.setShouldKillProcessSoftly(myShouldKillProcessSoftly);
    }
    return processHandler;
  }
}
