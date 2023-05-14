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

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.process.cmd.GeneralCommandLine;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see consulo.process.local.ProcessHandlerFactory#createProcessHandler(GeneralCommandLine, ProcessConsoleType)
 *
 * @author VISTALL
 * @since 01-Mar-22
 */
public enum ProcessConsoleType {
  BUILTIN(LocalizeValue.localizeTODO("Builtin")),
  EXTERNAL_EMULATION(LocalizeValue.localizeTODO("Builtin with external emulation")),
  /**
   * Supported only at windows
   */
  EXTERNAL(LocalizeValue.localizeTODO("External")) {
    @Override
    public boolean isConsoleViewSupported() {
      return false;
    }

    @Override
    public boolean isAvailable() {
      return Platform.current().os().isWindows();
    }
  };

  private final LocalizeValue myDisplayName;

  ProcessConsoleType(@Nonnull LocalizeValue displayName) {
    myDisplayName = displayName;
  }

  @Nonnull
  public LocalizeValue getDisplayName() {
    return myDisplayName;
  }

  public boolean isAvailable() {
    return true;
  }

  public boolean isConsoleViewSupported() {
    return true;
  }

  @Nonnull
  public static List<ProcessConsoleType> listSupported() {
    return Arrays.stream(values()).filter(ProcessConsoleType::isAvailable).collect(Collectors.toList());
  }
}
