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
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import consulo.application.impl.FrameTitleUtil;
import consulo.awt.TargetAWT;
import consulo.start.WelcomeFrameManager;
import consulo.ui.*;
import consulo.ui.app.impl.settings.SettingsDialog;
import consulo.ui.model.ListModel;
import consulo.ui.shared.Size;
import consulo.ui.shared.border.BorderPosition;
import consulo.web.application.WebApplication;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
@Singleton
public class WebWelcomeFrameManager implements WelcomeFrameManager {
  private Window myWindow;

  @Inject
  public WebWelcomeFrameManager(@Nonnull Application application) {
    application.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project) {
        Window window = myWindow;

        myWindow = null;

        if (window != null) {
          WebApplication.invokeOnCurrentSession(window::close);
        }
      }
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Window openFrame() {
    Window welcomeFrame = Window.createModal(FrameTitleUtil.buildTitle());
    welcomeFrame.setResizable(false);
    welcomeFrame.setClosable(false);
    welcomeFrame.setSize(WelcomeFrameManager.getDefaultWindowSize());
    welcomeFrame.setContent(Label.create("Loading..."));

    AnAction[] recentProjectsActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false);

    ListModel<AnAction> model = ListModel.create(Arrays.asList(recentProjectsActions));

    ListBox<AnAction> listSelect = ListBox.create(model);
    listSelect.setRender((render, index, item) -> render.append(((ReopenProjectAction)item).getProjectName()));
    listSelect.addValueListener(event -> {
      ReopenProjectAction value = (ReopenProjectAction)event.getValue();

      AnActionEvent e = AnActionEvent.createFromAnAction(value, null, ActionPlaces.WELCOME_SCREEN, DataManager.getInstance().getDataContext(welcomeFrame));

      value.actionPerformed(e);
    });
    listSelect.addBorder(BorderPosition.RIGHT);
    listSelect.setSize(new Size(300, -1));

    DockLayout layout = DockLayout.create();
    layout.left(listSelect);

    VerticalLayout projectActionLayout = VerticalLayout.create();

    ActionManager actionManager = ActionManager.getInstance();
    ActionGroup quickStart = (ActionGroup)actionManager.getAction(IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);
    List<AnAction> group = new ArrayList<>();
    collectAllActions(group, quickStart);

    for (AnAction action : group) {
      AnActionEvent e = AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, DataManager.getInstance().getDataContext(welcomeFrame));
      action.update(e);

      Presentation presentation = e.getPresentation();
      if (presentation.isVisible()) {
        String text = presentation.getText();
        if (text != null && text.endsWith("...")) {
          text = text.substring(0, text.length() - 3);
        }

        Hyperlink component = Hyperlink.create(text, () -> {
          action.actionPerformed(e);
        });

        component.setImage(TargetAWT.from(presentation.getIcon()));

        projectActionLayout.add(component);
      }
    }

    projectActionLayout.add(Button.create("Settings", () -> {
      Configurable[] configurables = ShowSettingsUtilImpl.buildConfigurables(null);

      SettingsDialog settingsDialog = new SettingsDialog(configurables);

      settingsDialog.show();
    }));

    layout.center(projectActionLayout);

    welcomeFrame.setContent(layout);
    myWindow = welcomeFrame;
    return welcomeFrame;
  }

  public static void collectAllActions(List<AnAction> group, ActionGroup actionGroup) {
    for (AnAction action : actionGroup.getChildren(null)) {
      if (action instanceof ActionGroup && !((ActionGroup)action).isPopup()) {
        collectAllActions(group, (ActionGroup)action);
      }
      else {
        group.add(action);
      }
    }
  }
}
