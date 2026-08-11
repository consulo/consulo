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
import consulo.project.event.ProjectManagerListener;
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

    application.getMessageBus().connect().subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project, UIAccess uiAccess) {
        closeFrame(uiAccess);

        refreshFrames();
      }

      @Override
      public void projectClosed(Project project, UIAccess uiAccess) {
        refreshFrames();
      }
    });
  }

  @RequiredUIAccess
  public @Nullable IdeFrame getCurrentFrame() {
    UIAccess.assertIsUIThread();
    return UIAccess.current().getUserData(IdeFrame.KEY);
  }

  @RequiredUIAccess
  protected void frameClosed() {
    UIAccess.current().putUserData(IdeFrame.KEY, null);
  }

  @RequiredUIAccess
  public void showFrame() {
    UIAccess.assertIsUIThread();

    UIAccess uiAccess = UIAccess.current();
    if (uiAccess.getUserData(IdeFrame.KEY) != null) {
      return;
    }

    // a refresh can ask while the close of this ui's frame is still on its way - what is on screen here is the
    // project, and a welcome must not be drawn over it
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (project.getUserData(UIAccess.KEY) == uiAccess) {
        return;
      }
    }

    IdeFrame frame = createFrame();
    uiAccess.putUserData(IdeFrame.KEY, frame);
    frame.getWindow().show();
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
  public void closeFrame(@Nullable UIAccess uiAccess) {
    IdeFrame frame = uiAccess == null ? null : uiAccess.getUserData(IdeFrame.KEY);
    if (frame == null) {
      return;
    }

    uiAccess.giveIfNeed(() -> frame.getWindow().close());
  }

  /**
   * A welcome screen lists which projects are open, and that answer changed for every ui but the one which caused
   * it. Rebuilt rather than repainted - the list is made of actions which read the state when they are created.
   */
  public void refreshFrames() {
    for (UIAccess uiAccess : UIAccess.listAll()) {
      if (uiAccess.getUserData(IdeFrame.KEY) == null) {
        continue;
      }

      // give, not giveIfNeed - that one asks whether the caller is a ui thread, not whether it is this ui, and
      // showFrame builds the frame for whichever ui it runs on
      uiAccess.give(() -> {
        closeFrame(uiAccess);
        showFrame();
      });
    }
  }

  public void showIfNoProjectOpened() {
    myApplication.invokeLater((DumbAwareRunnable)() -> {
      WindowManagerEx windowManager = (WindowManagerEx)WindowManager.getInstance();
      windowManager.disposeRootFrame();

      if (UIAccess.supportsMultipleUI()) {
        showFrame();
        return;
      }

      IdeFrame[] frames = windowManager.getAllProjectFrames();
      if (frames.length == 0) {
        showFrame();
      }
    }, myApplication.getNoneModalityState());
  }

  
  @RequiredUIAccess
  protected abstract IdeFrame createFrame();
}
