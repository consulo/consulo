/*
 * Copyright 2013-2021 consulo.io
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
package consulo.execution.console;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.process.RunnerMediator;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-10-23
 */
public enum ConsoleType {
  BUILTIN(LocalizeValue.localizeTODO("Builtin")) {
    @Nonnull
    @Override
    public ProcessHandler createHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
      return ProcessHandlerFactory.getInstance().createProcessHandler(commandLine);
    }

    @Override
    public boolean isAvailable() {
      return true;
    }
  },
  EXTERNAL_EMULATION(LocalizeValue.localizeTODO("Builtin with external emulation")) {
    @Nonnull
    @Override
    public ProcessHandler createHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
      PtyCommandLine ptyCommandLine = new PtyCommandLine(commandLine);
      OSProcessHandler handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(ptyCommandLine);
      handler.setHasPty(true);
      return handler;
    }

    @Override
    public boolean isAvailable() {
      return true;
    }
  },
  EXTERNAL(LocalizeValue.localizeTODO("External")) {
    @Nonnull
    @Override
    public ProcessHandler createHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
      Platform.OperatingSystem os = Platform.current().os();
      if (os.isWindows()) {
        return RunnerMediator.getInstance().createProcess(commandLine, true);
      }
      throw new UnsupportedOperationException();

    }

    @Override
    public boolean isAvailable() {
      return Platform.current().os().isWindows() && RunnerMediator.getRunnerPath() != null;
    }

    @Override
    public boolean isConsoleViewSupported() {
      return false;
    }
  };

  private final LocalizeValue myDisplayName;

  ConsoleType(@Nonnull LocalizeValue displayName) {
    myDisplayName = displayName;
  }

  @Nonnull
  public LocalizeValue getDisplayName() {
    return myDisplayName;
  }

  public abstract boolean isAvailable();

  public boolean isConsoleViewSupported() {
    return true;
  }

  @Nonnull
  public abstract ProcessHandler createHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException;

  @Nonnull
  public static List<ConsoleType> listSupported() {
    return Arrays.stream(values()).filter(ConsoleType::isAvailable).collect(Collectors.toList());
  }
}
