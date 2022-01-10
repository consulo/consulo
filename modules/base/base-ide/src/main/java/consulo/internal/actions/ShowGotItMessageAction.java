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
package consulo.internal.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.GotItMessage;
import com.intellij.ui.awt.RelativePoint;

import java.awt.*;

/**
 * @author VISTALL
 * @since 03.04.2015
 */
public class ShowGotItMessageAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    GotItMessage.createMessage("Test", "Test Message")
            .setDisposable(e.getProject())
            .show(new RelativePoint(WindowManager.getInstance().getFrame(e.getProject()), new Point(0, 0)), Balloon.Position.above);
  }
}
