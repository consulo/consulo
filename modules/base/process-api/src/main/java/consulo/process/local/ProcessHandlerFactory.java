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
package consulo.process.local;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.process.ExecutionException;
import consulo.process.ProcessConsoleType;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;

import javax.annotation.Nonnull;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ProcessHandlerFactory {

  public static ProcessHandlerFactory getInstance() {
    return Application.get().getInstance(ProcessHandlerFactory.class);
  }

  /**
   * Returns a new instance of the {@link OSProcessHandler}.
   */
  @Nonnull
  public final ProcessHandler createProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return createProcessHandler(commandLine, ProcessConsoleType.BUILTIN);
  }

  /**
   * Returns a new instance of the {@link OSProcessHandler} which is aware of ANSI coloring output.
   */
  @Nonnull
  public abstract ProcessHandler createColoredProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException;

  @Nonnull
  public abstract ProcessHandler createProcessHandler(@Nonnull GeneralCommandLine commandLine, @Nonnull ProcessConsoleType consoleType) throws ExecutionException;
}
