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

import com.intellij.Patches;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.mac.MacMainFrameDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public abstract class IdeFrameDecorator implements Disposable {
  @Nullable
  public static IdeFrameDecorator decorate(@Nonnull DesktopIdeFrameImpl frame) {
    assert Patches.USE_REFLECTION_TO_ACCESS_JDK9;

    // we can't use internal api for fullscreen
    if (SystemInfo.isJavaVersionAtLeast(9, 0, 0)) {
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

  protected DesktopIdeFrameImpl myFrame;

  protected IdeFrameDecorator(DesktopIdeFrameImpl frame) {
    myFrame = frame;
  }

  public abstract boolean isInFullScreen();

  @Nonnull
  public abstract AsyncResult<Void> toggleFullScreen(boolean state);

  @Override
  public void dispose() {
    myFrame = null;
  }

  protected void notifyFrameComponents(boolean state) {
    if (myFrame != null) {
      myFrame.getRootPane().putClientProperty(WindowManagerEx.FULL_SCREEN, state);
      myFrame.getJMenuBar().putClientProperty(WindowManagerEx.FULL_SCREEN, state);
    }
  }

  // AWT-based decorator
  private static class AWTFrameDecorator extends IdeFrameDecorator {
    private AWTFrameDecorator(@Nonnull DesktopIdeFrameImpl frame) {
      super(frame);
    }

    @Override
    public boolean isInFullScreen() {
      if (myFrame == null) return false;

      Rectangle frameBounds = myFrame.getBounds();
      GraphicsDevice device = ScreenUtil.getScreenDevice(frameBounds);
      return device != null && device.getDefaultConfiguration().getBounds().equals(frameBounds) && myFrame.isUndecorated();
    }

    @Nonnull
    @Override
    public AsyncResult<Void> toggleFullScreen(boolean state) {
      if (myFrame == null) return AsyncResult.rejected();

      GraphicsDevice device = ScreenUtil.getScreenDevice(myFrame.getBounds());
      if (device == null) return AsyncResult.rejected();

      try {
        myFrame.getRootPane().putClientProperty(ScreenUtil.DISPOSE_TEMPORARY, Boolean.TRUE);
        if (state) {
          myFrame.getRootPane().putClientProperty("oldBounds", myFrame.getBounds());
        }
        myFrame.dispose();
        myFrame.setUndecorated(state);
      }
      finally {
        if (state) {
          myFrame.setBounds(device.getDefaultConfiguration().getBounds());
        }
        else {
          Object o = myFrame.getRootPane().getClientProperty("oldBounds");
          if (o instanceof Rectangle) {
            myFrame.setBounds((Rectangle)o);
          }
        }
        myFrame.setVisible(true);
        myFrame.getRootPane().putClientProperty(ScreenUtil.DISPOSE_TEMPORARY, null);

        notifyFrameComponents(state);
      }
      return AsyncResult.resolved();
    }
  }

  // Extended WM Hints-based decorator
  private static class EWMHFrameDecorator extends IdeFrameDecorator {
    private Boolean myRequestedState = null;

    private EWMHFrameDecorator(DesktopIdeFrameImpl frame) {
      super(frame);
      frame.addComponentListener(new ComponentAdapter() {
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
      return myFrame != null && X11UiUtil.isInFullScreenMode(myFrame);
    }

    @Nonnull
    @Override
    public AsyncResult<Void> toggleFullScreen(boolean state) {
      if (myFrame != null) {
        myRequestedState = state;
        X11UiUtil.toggleFullScreenMode(myFrame);
      }
      return AsyncResult.resolved();
    }
  }
}
