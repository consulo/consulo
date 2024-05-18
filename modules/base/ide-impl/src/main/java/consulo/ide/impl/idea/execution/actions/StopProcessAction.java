/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.actions;

import consulo.application.AllIcons;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.KillableProcessHandler;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerStopper;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Sergey Simonchik
 */
public class StopProcessAction extends DumbAwareAction implements AnAction.TransparentUpdate {

  private final ProcessHandler myProcessHandler;

  public StopProcessAction(@Nonnull String text, @Nullable String description, @Nonnull ProcessHandler processHandler) {
    super(text, description, AllIcons.Actions.Suspend);
    myProcessHandler = processHandler;
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent e) {
    update(e.getPresentation(), getTemplatePresentation(), myProcessHandler);
  }

  public static void update(@Nonnull Presentation presentation,
                            @Nonnull Presentation templatePresentation,
                            @Nullable ProcessHandler processHandler) {
    boolean enable = false;
    Image icon = templatePresentation.getIcon();
    String description = templatePresentation.getDescription();
    if (processHandler != null && !processHandler.isProcessTerminated()) {
      enable = true;
      if (processHandler.isProcessTerminating() && processHandler instanceof KillableProcessHandler) {
        KillableProcessHandler killableProcess = (KillableProcessHandler) processHandler;
        if (killableProcess.canKillProcess()) {
          // 'force quite' action presentation
          icon = PlatformIconGroup.debuggerKillprocess();
          description = "Kill process";
        }
      }
    }
    presentation.setEnabled(enable);
    presentation.setIcon(icon);
    presentation.setDescription(description);
  }


  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    ProcessHandlerStopper.stop(myProcessHandler);
  }
}
