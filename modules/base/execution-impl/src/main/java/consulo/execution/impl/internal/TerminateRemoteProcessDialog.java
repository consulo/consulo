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

package consulo.execution.impl.internal;

import consulo.execution.ApplicationExecutionSettings;
import consulo.execution.ProcessCloseConfirmation;
import consulo.execution.impl.internal.ui.RunContentManagerImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.process.BaseProcessHandler;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ArrayUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TerminateRemoteProcessDialog {

  public static ProcessCloseConfirmation show(Project project, String sessionName, ProcessHandler processHandler) {
    //noinspection deprecation
    if (processHandler.isSilentlyDestroyOnClose() || Boolean.TRUE.equals(processHandler.getUserData(BaseProcessHandler.SILENTLY_DESTROY_ON_CLOSE))) {
      return ProcessCloseConfirmation.TERMINATE;
    }

    ApplicationExecutionSettings settings = project.getApplication().getInstance(ApplicationExecutionSettings.class);

    boolean canDisconnect = !Boolean.TRUE.equals(processHandler.getUserData(RunContentManagerImpl.ALWAYS_USE_DEFAULT_STOPPING_BEHAVIOUR_KEY));
    ProcessCloseConfirmation confirmation = settings.getProcessCloseConfirmation();
    if (confirmation != ProcessCloseConfirmation.ASK) {
      if (confirmation == ProcessCloseConfirmation.DISCONNECT && !canDisconnect) {
        confirmation = ProcessCloseConfirmation.TERMINATE;
      }
      return confirmation;
    }
    List<String> options = new ArrayList<>(3);
    options.add(ExecutionLocalize.buttonTerminate().get());
    if (canDisconnect) {
      options.add(ExecutionLocalize.buttonDisconnect().get());
    }
    options.add(CommonLocalize.buttonCancel().get());
    DialogWrapper.DoNotAskOption.Adapter doNotAskOption = new DialogWrapper.DoNotAskOption.Adapter() {
      @Override
      public void rememberChoice(boolean isSelected, int exitCode) {
        if (isSelected) {
          ProcessCloseConfirmation confirmation = getConfirmation(exitCode, canDisconnect);
          if (confirmation != null) {
            settings.setProcessCloseConfirmation(confirmation);
          }
        }
      }
    };

    AtomicBoolean alreadyGone = new AtomicBoolean(false);
    Runnable dialogRemover = Messages.createMessageDialogRemover(project);
    ProcessListener listener = new ProcessListener() {
      @Override
      public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
        alreadyGone.set(true);
        dialogRemover.run();
      }
    };
    processHandler.addProcessListener(listener);


    boolean defaultDisconnect = processHandler.detachIsDefault();
    int exitCode = Messages.showDialog(
      project,
      ExecutionLocalize.terminateProcessConfirmationText(sessionName).get(),
      ExecutionLocalize.processIsRunningDialogTitle(sessionName).get(),
      ArrayUtil.toStringArray(options),
      canDisconnect && defaultDisconnect ? 1 : 0,
      UIUtil.getWarningIcon(),
      doNotAskOption
    );
    processHandler.removeProcessListener(listener);
    if (alreadyGone.get()) {
      return ProcessCloseConfirmation.DISCONNECT;
    }
    return getConfirmation(exitCode, canDisconnect);
  }

  private static ProcessCloseConfirmation getConfirmation(int button, boolean withDisconnect) {
    switch (button) {
      case 0:
        return ProcessCloseConfirmation.TERMINATE;
      case 1:
        if (withDisconnect) {
          return ProcessCloseConfirmation.DISCONNECT;
        }
      default:
        return null;
    }
  }
}
