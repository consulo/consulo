/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author pegov
 */
public class ToggleFullScreenAction extends AnAction implements DumbAware {
  private static final String TEXT_ENTER_FULL_SCREEN = ActionsBundle.message("action.ToggleFullScreen.text.enter");
  private static final String TEXT_EXIT_FULL_SCREEN = ActionsBundle.message("action.ToggleFullScreen.text.exit");

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    IdeFrameEx frame = getFrame();
    if (frame != null) {
      frame.toggleFullScreen(!frame.isInFullScreen());
    }
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation p = e.getPresentation();

    IdeFrameEx frame = null;
    boolean isApplicable = WindowManager.getInstance().isFullScreenSupportedInCurrentOS() && (frame = getFrame()) != null;

    p.setEnabledAndVisible(isApplicable);

    if (isApplicable) {
      p.setText(frame.isInFullScreen() ? TEXT_EXIT_FULL_SCREEN : TEXT_ENTER_FULL_SCREEN);
    }
  }

  @Nullable
  private static IdeFrameEx getFrame() {
    Component focusOwner = IdeFocusManager.getGlobalInstance().getFocusOwner();
    if (focusOwner != null) {
      Window awtWindow = focusOwner instanceof JFrame ? (Window)focusOwner : SwingUtilities.getWindowAncestor(focusOwner);

      consulo.ui.Window window = TargetAWT.from(awtWindow);
      if (window != null) {
        return (IdeFrameEx)window.getUserData(IdeFrame.KEY);
      }
    }
    return null;
  }
}
