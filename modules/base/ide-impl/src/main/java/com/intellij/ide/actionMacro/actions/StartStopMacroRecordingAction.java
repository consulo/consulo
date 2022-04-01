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
package com.intellij.ide.actionMacro.actions;

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import com.intellij.ide.actionMacro.ActionMacroManager;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author max
 */
public class StartStopMacroRecordingAction extends AnAction implements DumbAware {
  public void update(AnActionEvent e) {
    boolean isRecording = ActionMacroManager.getInstance().isRecording();

    e.getPresentation().setText(isRecording
                                ? IdeBundle.message("action.stop.macro.recording")
                                : IdeBundle.message("action.start.macro.recording"));

    if (ActionPlaces.STATUS_BAR_PLACE.equals(e.getPlace())) {
      e.getPresentation().setIcon(AllIcons.Ide.Macro.Recording_stop);
    }
    else {
      e.getPresentation().setIcon(null);
    }
  }

  public void actionPerformed(AnActionEvent e) {
    if (!ActionMacroManager.getInstance().isRecording()) {
      final ActionMacroManager manager = ActionMacroManager.getInstance();
      manager.startRecording(IdeBundle.message("macro.noname"));
    }
    else {
      ActionMacroManager.getInstance().stopRecording(e.getData(CommonDataKeys.PROJECT));
    }
  }
}
