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

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.Size;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
public abstract class WelcomeFrameManager {
  @Nonnull
  public static WelcomeFrameManager getInstance() {
    return ServiceManager.getService(WelcomeFrameManager.class);
  }

  public static final String DIMENSION_KEY = "WELCOME_SCREEN";

  @Nonnull
  public static Size getDefaultWindowSize() {
    return new Size(800, 460);
  }

  public static boolean isFromWelcomeFrame(@Nonnull AnActionEvent e) {
    return e.getPlace().equals(ActionPlaces.WELCOME_SCREEN);
  }

  private IdeFrame myFrameInstance;

  private final Application myApplication;

  protected WelcomeFrameManager(Application application) {
    myApplication = application;

    application.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project, UIAccess uiAccess) {
        uiAccess.give(() -> closeFrame());
      }
    });
  }

  @Nullable
  @RequiredUIAccess
  public IdeFrame getCurrentFrame() {
    UIAccess.assertIsUIThread();
    return myFrameInstance;
  }

  protected void frameClosed() {
    myFrameInstance = null;
  }

  @RequiredUIAccess
  public void showFrame() {
    UIAccess.assertIsUIThread();

    if (myFrameInstance == null) {
      myFrameInstance = createFrame();
      myFrameInstance.getWindow().show();
    }
  }

  @RequiredUIAccess
  public void closeFrame() {
    UIAccess.assertIsUIThread();
    IdeFrame frameInstance = myFrameInstance;

    if (frameInstance == null) {
      return;
    }

    frameInstance.getWindow().close();
  }

  public void showIfNoProjectOpened() {
    myApplication.invokeLater((DumbAwareRunnable)() -> {
      WindowManagerEx windowManager = (WindowManagerEx)WindowManager.getInstance();
      windowManager.disposeRootFrame();
      IdeFrame[] frames = windowManager.getAllProjectFrames();
      if (frames.length == 0) {
        showFrame();
      }
    }, ModalityState.NON_MODAL);
  }

  @Nonnull
  @RequiredUIAccess
  protected abstract IdeFrame createFrame();
}
