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
package consulo.web.ui.welcomeFrame;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Presentation;
import consulo.application.impl.FrameTitleUtil;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.ui.Components;
import consulo.ui.DockLayout;
import consulo.ui.Layouts;
import consulo.ui.ListBox;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.VerticalLayout;
import consulo.ui.Window;
import consulo.ui.Windows;
import consulo.ui.border.BorderPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WelcomeFrameBuilder {
  @RequiredUIAccess
  public static void show() {

    Window welcomeFrame = Windows.modalWindow(FrameTitleUtil.buildTitle());
    welcomeFrame.setResizable(false);
    welcomeFrame.setClosable(false);
    welcomeFrame.setSize(new Size(777, 460));
    welcomeFrame.setContent(Components.label("TEst"));


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

    welcomeFrame.show();
  }
}
