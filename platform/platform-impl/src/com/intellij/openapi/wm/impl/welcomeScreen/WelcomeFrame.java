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
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WelcomeFrame {
  static final String DIMENSION_KEY = "WELCOME_SCREEN";
  private static IdeFrame ourInstance;

  public static IdeFrame getInstance() {
    return ourInstance;
  }

  public static void saveLocation(Rectangle location) {
    Point middle = new Point(location.x + location.width / 2, location.y = location.height / 2);
    DimensionService.getInstance().setLocationNoRealKey(WelcomeFrame.DIMENSION_KEY, middle);
  }

  static void setupCloseAction(final JFrame frame) {
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        WelcomeFrame.saveLocation(frame.getBounds());

        frame.dispose();

        if (ProjectManager.getInstance().getOpenProjects().length == 0) {
          ApplicationManagerEx.getApplicationEx().exit();
        }
      }
    });
  }

  public static void resetInstance() {
    ourInstance = null;
  }

  public static void showNow() {
    if (ourInstance == null) {
      IdeFrame frame = new FlatWelcomeFrame();
      ((JFrame)frame).setVisible(true);
      ourInstance = frame;
    }
  }

  public static void showIfNoProjectOpened() {
    ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
      @Override
      public void run() {
        WindowManagerImpl windowManager = (WindowManagerImpl)WindowManager.getInstance();
        windowManager.disposeRootFrame();
        IdeFrameImpl[] frames = windowManager.getAllProjectFrames();
        if (frames.length == 0) {
          showNow();
        }
      }
    }, ModalityState.NON_MODAL);
  }

  public static boolean isFromWelcomeFrame(@NotNull AnActionEvent e) {
    return e.getPlace() == ActionPlaces.WELCOME_SCREEN;
  }
}
