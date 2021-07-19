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
package consulo.start;

import com.intellij.ide.DataManager;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeFrame;
import consulo.application.impl.FrameTitleUtil;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.ListModel;
import consulo.wm.impl.UnifiedWelcomeIdeFrame;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
@Singleton
public class UnifiedWelcomeFrameManager extends WelcomeFrameManager {
  private final ProjectManager myProjectManager;
  private final RecentProjectsManager myRecentProjectsManager;
  private final DataManager myDataManager;

  @Inject
  public UnifiedWelcomeFrameManager(Application application, ProjectManager projectManager, RecentProjectsManager recentProjectsManager, DataManager dataManager) {
    super(application);
    myProjectManager = projectManager;
    myRecentProjectsManager = recentProjectsManager;
    myDataManager = dataManager;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public IdeFrame createFrame() {
    Window welcomeFrame = Window.create(FrameTitleUtil.buildTitle(), WindowOptions.builder().disableResize().build());
    welcomeFrame.addListener(Window.CloseListener.class, () -> frameClosed());
    welcomeFrame.setSize(WelcomeFrameManager.getDefaultWindowSize());
    welcomeFrame.setContent(Label.create("Loading..."));

    AnAction[] recentProjectsActions = myRecentProjectsManager.getRecentProjectsActions(false);

    ListModel<AnAction> model = ListModel.of(Arrays.asList(recentProjectsActions));

    ListBox<AnAction> listSelect = ListBox.create(model);
    listSelect.setRender((render, index, item) -> render.append(((ReopenProjectAction)item).getProjectName()));
    listSelect.addValueListener(event -> {
      ReopenProjectAction value = (ReopenProjectAction)event.getValue();

      AnActionEvent e = AnActionEvent.createFromAnAction(value, null, ActionPlaces.WELCOME_SCREEN, myDataManager.getDataContext(welcomeFrame));

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
      AnActionEvent e = AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, myDataManager.getDataContext(welcomeFrame));
      action.update(e);

      Presentation presentation = e.getPresentation();
      if (presentation.isVisible()) {
        String text = presentation.getText();
        if (text != null && text.endsWith("...")) {
          text = text.substring(0, text.length() - 3);
        }

        Hyperlink component = Hyperlink.create(text, (event) -> action.actionPerformed(e));

        component.setImage(presentation.getIcon());

        projectActionLayout.add(component);
      }
    }

    projectActionLayout.add(Button.create("Settings", (e) -> {
      ShowSettingsUtil.getInstance().showSettingsDialog(null);
    }));

    layout.center(projectActionLayout);

    welcomeFrame.setContent(layout);

    return new UnifiedWelcomeIdeFrame(welcomeFrame, myProjectManager.getDefaultProject());
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
