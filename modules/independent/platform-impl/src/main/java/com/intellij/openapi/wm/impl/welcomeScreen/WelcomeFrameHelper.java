/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.wm.impl.welcomeScreen;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import consulo.start.WelcomeFrameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO [VISTALL] merge with WelcomeFrameManager
@Singleton
public class WelcomeFrameHelper {
  @Deprecated
  public static WelcomeFrameHelper getInstance() {
    return Application.get().getComponent(WelcomeFrameHelper.class);
  }

  private IdeFrame myWelcomeFrame;

  private final Application myApplication;
  private final WelcomeFrameManager myWelcomeFrameManager;
  private final WindowManager myWindowManager;

  @Inject
  public WelcomeFrameHelper(Application application, WelcomeFrameManager welcomeFrameManager, WindowManager windowManager) {
    myApplication = application;
    myWelcomeFrameManager = welcomeFrameManager;
    myWindowManager = windowManager;
  }

  public IdeFrame getCurrentFrame() {
    return myWelcomeFrame;
  }

  public void resetInstance() {
    myWelcomeFrame = null;
  }

  public void showNow() {
    if (myWelcomeFrame == null) {
      myWelcomeFrame = (IdeFrame)myWelcomeFrameManager.createFrame();
    }
  }

  public void showIfNoProjectOpened() {
    myApplication.invokeLater((DumbAwareRunnable)() -> {
      WindowManagerEx windowManager = (WindowManagerEx)myWindowManager;
      windowManager.disposeRootFrame();
      IdeFrame[] frames = windowManager.getAllProjectFrames();
      if (frames.length == 0) {
        showNow();
      }
    }, ModalityState.NON_MODAL);
  }

  public boolean isFromWelcomeFrame(@Nonnull AnActionEvent e) {
    return e.getPlace().equals(ActionPlaces.WELCOME_SCREEN);
  }
}
