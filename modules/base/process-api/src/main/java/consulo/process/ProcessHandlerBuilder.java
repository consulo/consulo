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
package consulo.process;

import consulo.application.Application;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.io.BaseOutputReader;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/11/2022
 */
public interface ProcessHandlerBuilder {
  @Nonnull
  @Deprecated
  static ProcessHandlerBuilder create(@Nonnull GeneralCommandLine commandLine) {
    return Application.get().getInstance(ProcessHandlerBuilderFactory.class).newBuilder(commandLine);
  }

  /**
   * Almost all other methods will be ignored if process handler is binary
   *
   * {@link #build()} will return object instance of {@link BinaryProcessHandler}
   */
  @Nonnull
  ProcessHandlerBuilder binary();

  @Nonnull
  ProcessHandlerBuilder colored();

  /**
   * {@link #build()} will return object instance of {@link KillableProcessHandler}
   */
  @Nonnull
  ProcessHandlerBuilder killable();

  /**
   * {@link BaseOutputReader.Options#forMostlySilentProcess()}
   */
  @Nonnull
  ProcessHandlerBuilder silentReader();

  /**
   * {@link BaseOutputReader.Options#BLOCKING}
   */
  @Nonnull
  ProcessHandlerBuilder blockingReader();

  @Nonnull
  ProcessHandlerBuilder consoleType(@Nonnull ProcessConsoleType type);

  @Nonnull
  ProcessHandlerBuilder shouldDestroyProcessRecursively(boolean destroyRecursive);

  /**
   * Sets whether the process will be terminated gracefully.
   *
   * @param killProcessSoftly true, if graceful process termination should be attempted first (i.e. soft kill)
   */
  @Nonnull
  ProcessHandlerBuilder shouldKillProcessSoftly(boolean killProcessSoftly);

  @Nonnull
  ProcessHandler build() throws ExecutionException;
}
