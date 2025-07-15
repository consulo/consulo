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
package consulo.ide.impl.idea.ide.actionMacro.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.actionMacro.ActionMacroManager;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class StartStopMacroRecordingAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }

    if (!ActionMacroManager.getInstance().isRecording()) {
      final ActionMacroManager manager = ActionMacroManager.getInstance();
      manager.startRecording(project, IdeLocalize.macroNoname().get());
    }
    else {
      ActionMacroManager.getInstance().stopRecording(project);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY));

    boolean isRecording = ActionMacroManager.getInstance().isRecording();

    e.getPresentation().setTextValue(
        isRecording
            ? IdeLocalize.actionStopMacroRecording()
            : IdeLocalize.actionStartMacroRecording()
    );

    if (ActionPlaces.STATUS_BAR_PLACE.equals(e.getPlace())) {
      e.getPresentation().setIcon(PlatformIconGroup.actionsSuspend());
    }
    else {
      e.getPresentation().setIcon(null);
    }
  }
}
