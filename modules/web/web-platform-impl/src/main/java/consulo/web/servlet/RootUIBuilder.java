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
package consulo.web.servlet;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.disposer.Disposer;
import consulo.start.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.layout.DockLayout;
import consulo.ui.web.internal.ex.WebLoadingPanelImpl;
import consulo.ui.web.servlet.UIBuilder;
import consulo.ui.web.servlet.VaadinWebSessionImpl;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

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

    window.setContent(new WebLoadingPanelImpl());

    UIAccess access = UIAccess.current();

    scheduleWelcomeFrame(access, window);
  }

  private void scheduleWelcomeFrame(UIAccess access, Window window) {
    AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
      WebApplication application = WebApplication.getInstance();
      if (application == null || !((ApplicationEx)application).isLoaded()) {
        if (access.isValid()) {
          scheduleWelcomeFrame(access, window);
        }
        return;
      }

      if (access.isValid()) {
        access.give(() -> showWelcomeFrame(application, window));
      }
    }, 1, TimeUnit.SECONDS);
  }

  @RequiredUIAccess
  private void showWelcomeFrame(WebApplication application, Window window) {
    window.setContent(DockLayout.create());

    WebSession currentSession = application.getCurrentSession();
    if (currentSession != null) {
      WebSession newSession = currentSession;

      currentSession.close();

      currentSession = newSession.copy();
    }
    else {
      currentSession = new VaadinWebSessionImpl();
    }

    application.setCurrentSession(currentSession);

    WelcomeFrameManager.getInstance().showIfNoProjectOpened();
  }
}
