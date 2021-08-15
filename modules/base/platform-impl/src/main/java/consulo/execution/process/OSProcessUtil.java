/*
 * Copyright 2013-2017 consulo.io
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
package consulo.execution.process;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.util.text.StringUtil;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public class OSProcessUtil {
  private static final Logger LOG = Logger.getInstance(OSProcessUtil.class);

  @Deprecated
  public static int getCurrentProcessId() {
    return (int)ProcessHandle.current().pid();
  }

  public static int getProcessID(@Nonnull Process process) {
    return (int)process.pid();
  }

  @Nonnull
  public static List<ProcessInfo> getProcessList() {
    List<ProcessInfo> processInfos = new ArrayList<>();

    ProcessHandle.allProcesses().forEach(it -> {
      long pid = it.pid();

      try {
        ProcessHandle.Info info = it.info();

        Optional<String> commandOptional = info.command();
        // no access to process info
        if(commandOptional.isEmpty() && info.user().isEmpty()) {
          return;
        }

        String command = commandOptional.orElse("");
        String commandLine = info.commandLine().orElse("");
        String args = StringUtil.join(info.arguments().orElse(ArrayUtil.EMPTY_STRING_ARRAY), " ");

        processInfos.add(new ProcessInfo((int)pid, commandLine, new File(command).getName(), args, command));
      }
      catch (Throwable e) {
        LOG.warn("Failed to get #info() for processId: " + pid, e);
      }
    });

    return processInfos;
  }
}
