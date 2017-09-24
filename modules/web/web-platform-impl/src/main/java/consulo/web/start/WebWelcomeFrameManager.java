/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.start;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import consulo.application.impl.FrameTitleUtil;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.start.WelcomeFrameManager;
import consulo.ui.*;
import consulo.ui.border.BorderPosition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
public class WebWelcomeFrameManager implements WelcomeFrameManager {
  @RequiredUIAccess
  @NotNull
  @Override
  public Window createFrame() {
    Window welcomeFrame = Windows.modalWindow(FrameTitleUtil.buildTitle());
    welcomeFrame.setResizable(false);
    welcomeFrame.setClosable(false);
    welcomeFrame.setSize(WelcomeFrameManager.getDefaultWindowSize());
    welcomeFrame.setContent(Components.label("Test"));

    ListBox<String> listSelect = Components.listBox("Test");
    listSelect.addBorder(BorderPosition.RIGHT);
    listSelect.setSize(new Size(300, -1));

    DockLayout layout = Layouts.dock();
    layout.left(listSelect);

    VerticalLayout projectActionLayout = Layouts.vertical();

    ActionManager actionManager = ActionManager.getInstance();
    ActionGroup quickStart = (ActionGroup)actionManager.getAction(IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);
    List<AnAction> group = new ArrayList<>();
    FlatWelcomeScreen.collectAllActions(group, quickStart);

    for (AnAction action : group) {
      AnActionEvent e = AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, DataManager.getInstance().getDataContext2(welcomeFrame));
      action.update(e);

      Presentation presentation = e.getPresentation();
      if (presentation.isVisible()) {
        String text = presentation.getText();
        if (text != null && text.endsWith("...")) {
          text = text.substring(0, text.length() - 3);
        }

        projectActionLayout.add(Components.button(text, () -> {
          action.actionPerformed(e);
        }));
      }
    }

    layout.center(projectActionLayout);

    welcomeFrame.setContent(layout);
    return welcomeFrame;
  }
}
