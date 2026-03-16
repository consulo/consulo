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
package consulo.project.ui.wm;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.dumb.DumbAwareRunnable;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.WelcomeProjectManager;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.Size2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class WelcomeFrameManager {
  public static WelcomeFrameManager getInstance() {
    return Application.get().getInstance(WelcomeFrameManager.class);
  }

  public static final String DIMENSION_KEY = "WELCOME_SCREEN";

  public static Size2D getDefaultWindowSize() {
    return new Size2D(800, 460);
  }

  public static boolean isFromWelcomeFrame(AnActionEvent e) {
    return e.getPlace().equals(ActionPlaces.WELCOME_SCREEN);
  }

  protected final Application myApplication;

  protected WelcomeFrameManager(Application application) {
    myApplication = application;
  }

  @Nullable
  @RequiredUIAccess
  public IdeFrame getCurrentFrame() {
    UIAccess.assertIsUIThread();
    WelcomeProjectManager welcomeProjectManager = WelcomeProjectManager.getInstance();
    Project welcomeProject = welcomeProjectManager.getOpenWelcomeProject();
    if (welcomeProject != null) {
      return WindowManager.getInstance().getIdeFrame(welcomeProject);
    }
    return null;
  }

  @RequiredUIAccess
  public void showFrame() {
    WelcomeProjectManager.getInstance().openWelcomeProjectAsync(UIAccess.current());
  }

  @RequiredUIAccess
  public void closeFrame() {
    UIAccess.assertIsUIThread();
    WelcomeProjectManager.getInstance().closeWelcomeProjectAsync(UIAccess.current());
  }

  public void showIfNoProjectOpened() {
    myApplication.invokeLater((DumbAwareRunnable)() -> {
      WindowManagerEx windowManager = (WindowManagerEx)WindowManager.getInstance();
      windowManager.disposeRootFrame();

      // Check if there are any non-welcome project frames open
      Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
      boolean hasRealProject = false;
      for (Project project : openProjects) {
        if (!project.isWelcomeProject() && !project.isDefault()) {
          hasRealProject = true;
          break;
        }
      }

      if (!hasRealProject) {
        showFrame();
      }
    }, myApplication.getNoneModalityState());
  }
}
