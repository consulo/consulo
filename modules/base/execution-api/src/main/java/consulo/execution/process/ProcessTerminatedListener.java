/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.process.localize.ProcessLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.util.dataholder.Key;

/**
 * @author dyoma
 */
public class ProcessTerminatedListener extends ProcessAdapter {
  private static final Key<ProcessTerminatedListener> KEY = Key.create("processTerminatedListener");
  private final String myProcessFinishedMessage;
  private final Project myProject;
  protected static final String EXIT_CODE_ENTRY = "$EXIT_CODE$";
  protected static final String EXIT_CODE_REGEX = "\\$EXIT_CODE\\$";

  private ProcessTerminatedListener(final Project project, final String processFinishedMessage) {
    myProject = project;
    myProcessFinishedMessage = processFinishedMessage;
  }

  public static void attach(final ProcessHandler processHandler, Project project, final String message) {
    final ProcessTerminatedListener previousListener = processHandler.getUserData(KEY);
    if (previousListener != null) {
      processHandler.removeProcessListener(previousListener);
      if (project == null) project = previousListener.myProject;
    }

    final ProcessTerminatedListener listener = new ProcessTerminatedListener(project, message);
    processHandler.addProcessListener(listener);
    processHandler.putUserData(KEY, listener);
  }

  public static void attach(final ProcessHandler processHandler, final Project project) {
    String message = ProcessLocalize.finishedWithExitCodeTextMessage(EXIT_CODE_ENTRY).get();
    attach(processHandler, project, "\n" + message + "\n");
  }

  public static void attach(final ProcessHandler processHandler) {
    attach(processHandler, null);
  }

  public void processTerminated(ProcessEvent event) {
    final ProcessHandler processHandler = event.getProcessHandler();
    processHandler.removeProcessListener(this);
    final String message = myProcessFinishedMessage.replaceAll(EXIT_CODE_REGEX, String.valueOf(event.getExitCode()));
    processHandler.notifyTextAvailable(message, ProcessOutputTypes.SYSTEM);
    if (myProject != null) {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (myProject.isDisposed()) return;
        StatusBar.Info.set(message, myProject);
      });
    }
  }
}
