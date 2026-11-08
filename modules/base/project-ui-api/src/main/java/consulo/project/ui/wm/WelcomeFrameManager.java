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
import consulo.project.ProjectType;
import consulo.project.event.ProjectManagerListener;
import consulo.project.internal.ProjectOpenService;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIAccessInternal;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Shows the welcome project - a temporary project in a regular frame - whenever a ui has no project open.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class WelcomeFrameManager {
  public static WelcomeFrameManager getInstance() {
    return Application.get().getInstance(WelcomeFrameManager.class);
  }

  private static final Key<CompletableFuture<Project>> WELCOME_PROJECT_OPENING = Key.create("WelcomeFrameManager.welcomeProjectOpening");

  private static final Key<Boolean> WELCOME_PROJECT_CLOSING = Key.create("WelcomeFrameManager.welcomeProjectClosing");

  protected final Application myApplication;

  protected WelcomeFrameManager(Application application) {
    myApplication = application;

    application.getMessageBus().connect().subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project, UIAccess uiAccess) {
        if (project.getType() == ProjectType.WELCOME) {
          return;
        }

        closeFrame(uiAccess);
      }
    });
  }

  @RequiredUIAccess
  public void showFrame() {
    UIAccess.assertIsUIThread();

    UIAccess uiAccess = UIAccess.current();

    // a refresh can ask while the close of this ui's frame is still on its way - what is on screen here is the
    // project, and a welcome must not be drawn over it
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      UIAccess shownIn = project.getUserData(UIAccess.KEY);
      if (shownIn != null && UIAccessInternal.original(shownIn) == uiAccess) {
        return;
      }
    }

    openWelcomeProject(uiAccess);
  }

  @RequiredUIAccess
  public void closeFrame() {
    UIAccess.assertIsUIThread();

    closeFrame(UIAccess.current());
  }

  /**
   * The ui is named rather than taken from the caller, since a project is opened through steps which do not all
   * run with the ui of that project as the current one.
   */
  public void closeFrame(UIAccess uiAccess) {
    closeWelcomeProject(uiAccess);
  }

  public void showIfNoProjectOpened() {
    myApplication.invokeLater((DumbAwareRunnable)() -> {
      WindowManagerEx windowManager = (WindowManagerEx)WindowManager.getInstance();

      // the frame of the closed project is kept for the welcome project to move into
      if (UIAccess.supportsMultipleUI() || ProjectManager.getInstance().getOpenProjects().length == 0) {
        showFrame();
      }
      else {
        windowManager.disposeRootFrame();
      }
    }, myApplication.getNoneModalityState());
  }

  @RequiredUIAccess
  private void openWelcomeProject(UIAccess uiAccess) {
    if (uiAccess.getUserData(WELCOME_PROJECT_OPENING) != null) {
      return;
    }

    CompletableFuture<Project> opening = myApplication.getInstance(ProjectOpenService.class).openWelcomeProjectAsync(uiAccess);
    uiAccess.putUserData(WELCOME_PROJECT_OPENING, opening);
    opening.whenComplete((project, throwable) -> uiAccess.putUserData(WELCOME_PROJECT_OPENING, null));
  }

  private void closeWelcomeProject(UIAccess uiAccess) {
    // a welcome project still being set up is closed once it is, not from under its own initialization
    CompletableFuture<Project> opening = uiAccess.getUserData(WELCOME_PROJECT_OPENING);
    if (opening != null && !opening.isDone()) {
      opening.whenComplete((project, throwable) -> uiAccess.give(() -> closeWelcomeProject(uiAccess)));
      return;
    }

    Project project = findWelcomeProject(uiAccess);
    if (project == null) {
      return;
    }

    project.putUserData(WELCOME_PROJECT_CLOSING, Boolean.TRUE);
    uiAccess.giveIfNeed(() -> ProjectManager.getInstance().closeAndDisposeAsync(project, uiAccess, false, false, true));
  }

  /**
   * The welcome project shown in the given ui, unless it is already on its way out.
   */
  public @Nullable Project findWelcomeProject(UIAccess uiAccess) {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (project.getType() != ProjectType.WELCOME || project.getUserData(WELCOME_PROJECT_CLOSING) != null) {
        continue;
      }

      UIAccess shownIn = project.getUserData(UIAccess.KEY);
      if (shownIn != null && UIAccessInternal.original(shownIn) == UIAccessInternal.original(uiAccess)) {
        return project;
      }
    }
    return null;
  }
}
