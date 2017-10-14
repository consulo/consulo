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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import consulo.start.WelcomeFrameManager;
import org.jetbrains.annotations.NotNull;

public class WelcomeFrame {
  static final String DIMENSION_KEY = "WELCOME_SCREEN";
  private static IdeFrame ourInstance;

  public static IdeFrame getInstance() {
    return ourInstance;
  }

  public static void resetInstance() {
    ourInstance = null;
  }

  public static void showNow() {
    if (ourInstance == null) {
      ourInstance = (IdeFrame)WelcomeFrameManager.getInstance().openFrame();
    }
  }

  public static void showIfNoProjectOpened() {
    ApplicationManager.getApplication().invokeLater((DumbAwareRunnable)() -> {
      WindowManagerEx windowManager = (WindowManagerEx)WindowManager.getInstance();
      windowManager.disposeRootFrame();
      IdeFrame[] frames = windowManager.getAllProjectFrames();
      if (frames.length == 0) {
        showNow();
      }
    }, ModalityState.NON_MODAL);
  }

  public static boolean isFromWelcomeFrame(@NotNull AnActionEvent e) {
    return e.getPlace().equals(ActionPlaces.WELCOME_SCREEN);
  }
}
