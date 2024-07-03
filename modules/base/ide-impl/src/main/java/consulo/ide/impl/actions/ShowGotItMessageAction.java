/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.actions;

import consulo.ide.impl.idea.ui.GotItMessage;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.Balloon;

import java.awt.*;

/**
 * @author VISTALL
 * @since 03.04.2015
 */
public class ShowGotItMessageAction extends AnAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    GotItMessage.createMessage("Test", "Test ReflectionMessage")
      .setDisposable(e.getData(Project.KEY))
      .show(new RelativePoint(WindowManager.getInstance().getFrame(e.getData(Project.KEY)), new Point(0, 0)), Balloon.Position.above);
  }
}
