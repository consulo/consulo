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
import com.jezhumble.javasysmon.JavaSysMon;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public class OSProcessUtil {
  @NotNull
  public static ProcessInfo[] getProcessList() {
    JavaSysMon javaSysMon = new JavaSysMon();
    com.jezhumble.javasysmon.ProcessInfo[] processInfos = javaSysMon.processTable();
    ProcessInfo[] infos = new ProcessInfo[processInfos.length];
    for (int i = 0; i < processInfos.length; i++) {
      com.jezhumble.javasysmon.ProcessInfo info = processInfos[i];

      String executable;
      String args;
      List<String> commandLineList = StringUtil.splitHonorQuotes(info.getCommand(), ' ');
      if (commandLineList.isEmpty()) {
        executable = info.getName();
        args = "";
      }
      else {
        executable = commandLineList.get(0);
        if (commandLineList.size() > 1) {
          args = StringUtil.join(commandLineList.subList(1, commandLineList.size()), " ");
        }
        else {
          args = "";
        }
      }

      infos[i] = new ProcessInfo(info.getPid(), info.getCommand(), StringUtil.unquoteString(executable, '\"'), args);
    }
    return infos;
  }
}
