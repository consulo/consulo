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

import com.vaadin.flow.component.UI;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationEx;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.externalService.internal.startup.StartupCustomizeManager;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.lang.TimeoutUtil;
import consulo.web.ui.impl.internal.WebApplicationImpl;

import java.util.concurrent.CancellationException;

/**
 * @author VISTALL
 * @since 10-Sep-17
 */
public class RootUIBuilder implements UIBuilder {
  @RequiredUIAccess
  @Override
  public void build(Window window) {
//    Disposer.register(UIServlet.getDisposable(UI.getCurrent()), () -> {
//      Application application = ApplicationManager.getApplication();
//      if (application == null || !((ApplicationEx)application).isLoaded()) {
//        return;
//      }
//
//      UIAccess uiAccess = application.getLastUIAccess();
//      ProjectManager projectManager = ProjectManager.getInstance();
//
//      for (Project openProject : projectManager.getOpenProjects()) {
//        projectManager.closeAndDisposeAsync(openProject, uiAccess);
//      }
//    });

    // TODO window.setContent(new WebLoadingPanelImpl());

    UIAccess access = UIAccess.current();

    if (!giveWelcomeFrameIfLoaded(access, window)) {
      scheduleWelcomeFrame(access, window, 0);
    }
  }

  private static boolean giveWelcomeFrameIfLoaded(UIAccess access, Window window) {
    Application application = ApplicationManager.getApplication();
    if (application == null || !((ApplicationEx)application).isLoaded()) {
      return false;
    }

    if (access.isValid()) {
      access.give(() -> showWelcomeFrame(application, window));
    }
    return true;
  }

  private void scheduleWelcomeFrame(UIAccess access, Window window, int tryCount) {
    new Thread(() -> {
      TimeoutUtil.sleep(250L);

      if (giveWelcomeFrameIfLoaded(access, window) || !access.isValid()) {
        return;
      }

      scheduleWelcomeFrame(access, window, tryCount + 1);
    }, "UI Starter=" + tryCount).start();
  }

  @RequiredUIAccess
  private static void showWelcomeFrame(Application application, Window window) {
    window.setContent(DockLayout.create());

    WelcomeFrameManager welcomeFrameManager = WelcomeFrameManager.getInstance();

    UIAccess uiAccess = UIAccess.current();
    if (application instanceof WebApplicationImpl webApplication) {
      webApplication.registerUIAccess(uiAccess);
    }

    StartupCustomizeManager customizeManager = application.getInstance(StartupCustomizeManager.class);
    if (customizeManager.isPending()) {
      customizeManager.showAsync(true).whenCompleteAsync((result, error) -> {
        if (error instanceof CancellationException) {
          if (UI.getCurrent().getCurrentView() instanceof VaadinRootLayout rootLayout) {
            rootLayout.showClosed();
          }
          return;
        }

        welcomeFrameManager.showIfNoProjectOpened();
      }, uiAccess);
    }
    else {
      welcomeFrameManager.showIfNoProjectOpened();
    }
  }
}
