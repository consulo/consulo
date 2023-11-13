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
package consulo.web.internal.servlet;

import consulo.application.WriteAction;
import consulo.application.internal.ApplicationEx;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.lang.TimeoutUtil;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10-Sep-17
 */
public class RootUIBuilder implements UIBuilder {
  @RequiredUIAccess
  @Override
  public void build(@Nonnull Window window) {
    Disposer.register(window, () -> {
      WebApplication application = WebApplication.getInstance();
      if (application == null || !((ApplicationEx)application).isLoaded()) {
        return;
      }

      WriteAction.run(() -> {
        ProjectManager projectManager = ProjectManager.getInstance();

        Project[] openProjects = projectManager.getOpenProjects();

        for (Project openProject : openProjects) {
          projectManager.closeProject(openProject);
        }
      });
    });

    // TODO window.setContent(new WebLoadingPanelImpl());

    UIAccess access = UIAccess.current();

    scheduleWelcomeFrame(access, window, 0);
  }

  private void scheduleWelcomeFrame(UIAccess access, Window window, int tryCount) {
    new Thread(() -> {
      TimeoutUtil.sleep(1000L);
      
      WebApplication application = WebApplication.getInstance();
      if (application == null || !((ApplicationEx)application).isLoaded()) {
        if (access.isValid()) {
          scheduleWelcomeFrame(access, window, tryCount + 1);
        }
        return;
      }

      if (access.isValid()) {
        access.give(() -> showWelcomeFrame(application, window));
      }
    }, "UI Starter=" + tryCount).start();
  }

  @RequiredUIAccess
  private void showWelcomeFrame(WebApplication application, Window window) {
    window.setContent(DockLayout.create());

    WelcomeFrameManager welcomeFrameManager = WelcomeFrameManager.getInstance();

    WebSession currentSession = application.getCurrentSession();
    if (currentSession != null) {
      WebSession newSession = currentSession;

      currentSession.close();

      currentSession = newSession.copy();

      welcomeFrameManager.closeFrame();
    }
    else {
      currentSession = new VaadinWebSessionImpl();
    }

    application.setCurrentSession(currentSession);

    welcomeFrameManager.showIfNoProjectOpened();
  }
}
