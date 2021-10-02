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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.mac.MacMainFrameDecorator;
import consulo.annotation.ReviewAfterMigrationToJRE;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ui.Window;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public abstract class IdeFrameDecorator implements Disposable {
  public static boolean isCustomDecorationActive() {
    // not implemented
    return false;
  }

  @Nullable
  @ReviewAfterMigrationToJRE(9)
  public static IdeFrameDecorator decorate(@Nonnull IdeFrameEx frame) {
    // we can't use internal api for fullscreen
    if (SystemInfo.isJavaVersionAtLeast(9)) {
      return new AWTFrameDecorator(frame);
    }

    if (SystemInfo.isMac) {
      return new MacMainFrameDecorator(frame, false);
    }
    else if (SystemInfo.isWindows) {
      return new AWTFrameDecorator(frame);
    }
    else if (SystemInfo.isXWindow) {
      if (X11UiUtil.isFullScreenSupported()) {
        return new EWMHFrameDecorator(frame);
      }
    }

    return null;
  }

  protected IdeFrameEx myIdeFrame;

  protected IdeFrameDecorator(IdeFrameEx ideFrame) {
    myIdeFrame = ideFrame;
  }

  @Nullable
  protected JFrame getJFrame() {
    if(myIdeFrame == null) {
      return null;
    }
    Window window = myIdeFrame.getWindow();
    return (JFrame)TargetAWT.to(window);
  }

  public abstract boolean isInFullScreen();

  @Nonnull
  public abstract ActionCallback toggleFullScreen(boolean state);

  @Override
  public void dispose() {
    myIdeFrame = null;
  }

  protected void notifyFrameComponents(boolean state) {
    JFrame jFrame = getJFrame();
    if (jFrame == null) {
      return;
    }

    jFrame.getRootPane().putClientProperty(WindowManagerEx.FULL_SCREEN, state);
    jFrame.getJMenuBar().putClientProperty(WindowManagerEx.FULL_SCREEN, state);
  }

  // AWT-based decorator
  private static class AWTFrameDecorator extends IdeFrameDecorator {
    private AWTFrameDecorator(@Nonnull IdeFrameEx frame) {
      super(frame);
    }

    @Override
    public boolean isInFullScreen() {
      JFrame jFrame = getJFrame();
      if (jFrame == null) {
        return false;
      }

      Rectangle frameBounds = jFrame.getBounds();
      GraphicsDevice device = ScreenUtil.getScreenDevice(frameBounds);
      return device != null && device.getDefaultConfiguration().getBounds().equals(frameBounds) && jFrame.isUndecorated();
    }

    @Nonnull
    @Override
    public ActionCallback toggleFullScreen(boolean state) {
      JFrame jFrame = getJFrame();
      if (jFrame == null) {
        return ActionCallback.REJECTED;
      }

      GraphicsDevice device = ScreenUtil.getScreenDevice(jFrame.getBounds());
      if (device == null) return ActionCallback.REJECTED;

      try {
        jFrame.getRootPane().putClientProperty(ScreenUtil.DISPOSE_TEMPORARY, Boolean.TRUE);
        if (state) {
          jFrame.getRootPane().putClientProperty("oldBounds", jFrame.getBounds());
        }
        jFrame.dispose();
        jFrame.setUndecorated(state);
      }
      finally {
        if (state) {
          jFrame.setBounds(device.getDefaultConfiguration().getBounds());
        }
        else {
          Object o = jFrame.getRootPane().getClientProperty("oldBounds");
          if (o instanceof Rectangle) {
            jFrame.setBounds((Rectangle)o);
          }
        }
        jFrame.setVisible(true);
        jFrame.getRootPane().putClientProperty(ScreenUtil.DISPOSE_TEMPORARY, null);

        notifyFrameComponents(state);
      }
      return ActionCallback.DONE;
    }
  }

  // Extended WM Hints-based decorator
  private static class EWMHFrameDecorator extends IdeFrameDecorator {
    private Boolean myRequestedState = null;

    private EWMHFrameDecorator(IdeFrameEx frame) {
      super(frame);
      JFrame jFrame = getJFrame();
      assert jFrame != null;
      jFrame.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          if (myRequestedState != null) {
            notifyFrameComponents(myRequestedState);
            myRequestedState = null;
          }
        }
      });
    }

    @Override
    public boolean isInFullScreen() {
      JFrame jFrame = getJFrame();
      return jFrame != null && X11UiUtil.isInFullScreenMode(jFrame);
    }

    @Nonnull
    @Override
    public ActionCallback toggleFullScreen(boolean state) {
      JFrame jFrame = getJFrame();
      if (jFrame != null) {
        myRequestedState = state;
        X11UiUtil.toggleFullScreenMode(jFrame);
      }
      return ActionCallback.DONE;
    }
  }
}
