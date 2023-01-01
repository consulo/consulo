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
package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.PlatformDataKeys;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.codeEditor.Editor;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.RelativePoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author Vladimir Kondratyev
 */
public class ShowPopupMenuAction extends AnAction implements DumbAware, PopupAction {
  public ShowPopupMenuAction() {
    setEnabledInModalContext(true);
  }

  public void actionPerformed(AnActionEvent e) {
    final RelativePoint relPoint = JBPopupFactory.getInstance().guessBestPopupLocation(e.getDataContext());

    KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    Component focusOwner = focusManager.getFocusOwner();

    Point popupMenuPoint = relPoint.getPoint(focusOwner);

    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    int coord = editor != null
                ? Math.max(0, popupMenuPoint.y - 1) //To avoid cursor jump to the line below. http://www.jetbrains.net/jira/browse/IDEADEV-10644
                : popupMenuPoint.y;

    focusOwner.dispatchEvent(
      new MouseEvent(
        focusOwner,
        MouseEvent.MOUSE_PRESSED,
        System.currentTimeMillis(), 0,
        popupMenuPoint.x,
        coord,
        1,
        true
      )
    );
  }

  public void update(AnActionEvent e) {
    KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    e.getPresentation().setEnabled(focusManager.getFocusOwner() instanceof JComponent);
  }
}