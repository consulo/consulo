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
package com.intellij.ui;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import consulo.awt.TargetAWT;
import consulo.awt.hacking.AWTAccessorHacking;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * @author Sergey Malenkov
 */
public class FrameState {
  private Rectangle myBounds;
  private boolean myMaximized;
  private boolean myFullScreen;

  public Point getLocation() {
    return myBounds == null ? null : myBounds.getLocation();
  }

  public Dimension getSize() {
    return myBounds == null ? null : myBounds.getSize();
  }

  public Rectangle getBounds() {
    return myBounds == null ? null : new Rectangle(myBounds);
  }

  public boolean isMaximized() {
    return myMaximized;
  }

  public boolean isFullScreen() {
    return myFullScreen;
  }

  public static int getExtendedState(Component component) {
    int state = Frame.NORMAL;
    if (component instanceof Frame) {
      state = ((Frame)component).getExtendedState();
      if (SystemInfo.isMacOSLion) {
        state = AWTAccessorHacking.getExtendedStateFromPeer((Frame)component);
      }
    }
    return state;
  }

  public static boolean isFullScreen(Component component) {
    if(component instanceof Window) {
      consulo.ui.Window uiWindow = TargetAWT.from((Window)component);

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);

      if(ideFrame instanceof IdeFrameEx) {
         return WindowManager.getInstance().isFullScreenSupportedInCurrentOS() && ((IdeFrameEx)ideFrame).isInFullScreen();
      }
    }
    return false;
  }

  public static boolean isMaximized(int state) {
    return (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
  }

  private static FrameState findFrameState(@Nonnull Component component) {
    for (ComponentListener listener : component.getComponentListeners()) {
      if (listener instanceof FrameState) {
        return (FrameState)listener;
      }
    }
    return null;
  }

  public static FrameState getFrameState(@Nonnull Component component) {
    FrameState state = findFrameState(component);
    if (state == null) {
      state = new FrameState();
    }
    if (state.myBounds == null) {
      state.update(component);
    }
    return state;
  }

  public static void setFrameStateListener(@Nonnull Component component) {
    if (component instanceof Frame) {
      // it makes sense for a frame only
      FrameState state = findFrameState(component);
      if (state == null) {
        component.addComponentListener(new Listener());
      }
    }
  }

  private static final class Listener extends FrameState implements ComponentListener {
    @Override
    public void componentMoved(ComponentEvent event) {
      update(event.getComponent());
    }

    @Override
    public void componentResized(ComponentEvent event) {
      update(event.getComponent());
    }

    @Override
    public void componentShown(ComponentEvent event) {
    }

    @Override
    public void componentHidden(ComponentEvent event) {
    }
  }

  final void update(Component component) {
    Rectangle bounds = component.getBounds();
    myFullScreen = isFullScreen(component);
    myMaximized = isMaximized(getExtendedState(component));
    if (myBounds != null) {
      if (myFullScreen || myMaximized) {
        if (bounds.contains(myBounds.x + myBounds.width / 2, myBounds.y + myBounds.height / 2)) {
          return; // preserve old bounds for the maximized frame if its state can be restored
        }
      }
    }
    myBounds = bounds;
  }
}
