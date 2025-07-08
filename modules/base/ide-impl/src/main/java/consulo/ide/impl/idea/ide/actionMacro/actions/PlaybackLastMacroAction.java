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

import consulo.ide.impl.idea.ide.actionMacro.ActionMacroManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class PlaybackLastMacroAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ActionMacroManager.getInstance().playbackLastMacro();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    ActionMacroManager macroManager = ActionMacroManager.getInstance();
    e.getPresentation().setEnabled(!macroManager.isPlaying() && macroManager.hasRecentMacro());
  }
}
