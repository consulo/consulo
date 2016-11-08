/*
 * Copyright 2013-2016 must-be.org
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

package com.intellij.execution.runners;

import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 08-Nov-16
 *
 * Convert from Kotlin intellij-community\platform\lang-impl\src\com\intellij\execution\runners\ConsoleTitleGen.kt
 */
public class ConsoleTitleGen {
  private Project myProject;
  private String myConsoleTitle;
  private boolean myShouldAddNumberToTitle;

  public ConsoleTitleGen(Project project, String consoleTitle) {
    this(project, consoleTitle, true);
  }

  public ConsoleTitleGen(Project project, String consoleTitle, boolean shouldAddNumberToTitle) {
    myProject = project;
    myConsoleTitle = consoleTitle;
    myShouldAddNumberToTitle = shouldAddNumberToTitle;
  }

  public String makeTitle() {
    if (myShouldAddNumberToTitle) {
      List<String> activeConsoleNames = getActiveConsoles(myConsoleTitle);
      int max = 0;
      for (String name : activeConsoleNames) {
        if (max == 0) {
          max = 1;
        }
        try {
          int num = Integer.parseInt(name.substring(myConsoleTitle.length() + 1, name.length() - 1));
          if (num > max) {
            max = num;
          }
        }
        catch (Exception ignored) {
          //skip
        }

      }
      if (max >= 1) {
        return myConsoleTitle + "(" + (max + 1) + ")";
      }
    }

    return myConsoleTitle;
  }

  protected List<String> getActiveConsoles(String consoleTitle) {
    List<RunContentDescriptor> consoles = ExecutionHelper.collectConsolesByDisplayName(myProject, s -> s.contains(consoleTitle));

    return consoles.stream().filter(r -> {
      ProcessHandler processHandler = r.getProcessHandler();
      return processHandler != null && !processHandler.isProcessTerminated();
    }).map(RunContentDescriptor::getDisplayName).collect(Collectors.toList());
  }
}
