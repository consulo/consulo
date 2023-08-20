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
package consulo.desktop.awt.wm.impl;

import consulo.awt.hacking.X11Hacking;
import consulo.util.concurrent.ActionCallback;
import consulo.application.util.SystemInfo;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.annotation.ReviewAfterMigrationToJRE;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ui.Window;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    if (SystemInfo.isXWindow && X11UiUtil.isFullScreenSupported()) {
      return new EWMHFrameDecorator(frame);
    }

    if (SystemInfo.isMac) {
      return new MacMainFrameDecorator(frame, false);
    }

    return new AWTFrameDecorator(frame);
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
      return jFrame != null && X11Hacking.isInFullScreenMode(jFrame);
    }

    @Nonnull
    @Override
    public ActionCallback toggleFullScreen(boolean state) {
      JFrame jFrame = getJFrame();
      if (jFrame != null) {
        myRequestedState = state;
        X11Hacking.toggleFullScreenMode(jFrame);
      }
      return ActionCallback.DONE;
    }
  }
}
