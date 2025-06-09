// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.application.ui.impl;

import consulo.application.ui.impl.internal.BaseWindowStateBean;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import java.awt.*;

final class WindowStateBean extends BaseWindowStateBean {
  @Override
  public void setMaximized(boolean maximized) {
    setExtendedState(maximized ? Frame.MAXIMIZED_BOTH : Frame.NORMAL);
  }

  @Override
  public void applyTo(@Nonnull Window window) {
    Point2D location = getLocation();
    Size2D size = getSize();
    int extendedState = getExtendedState();

    Frame frame = window instanceof Frame ? (Frame)window : null;
    if (frame != null && Frame.NORMAL != frame.getExtendedState()) {
      frame.setExtendedState(Frame.NORMAL);
    }

    Rectangle bounds = window.getBounds();
    if (location != null) bounds.setLocation(TargetAWT.to(location));
    if (size != null) bounds.setSize(TargetAWT.to(size));
    if (bounds.isEmpty()) bounds.setSize(window.getPreferredSize());
    window.setBounds(bounds);

    if (frame != null && Frame.NORMAL != extendedState) {
      frame.setExtendedState(extendedState);
    }
  }

  void applyFrom(@Nonnull Window window) {
    if (window.isVisible()) {
      boolean windowFullScreen = isFullScreen(window);
      setFullScreen(windowFullScreen);

      Frame frame = window instanceof Frame ? (Frame)window : null;
      int windowExtendedState = frame == null ? Frame.NORMAL : frame.getExtendedState();
      setExtendedState(windowExtendedState);

      if (!windowFullScreen && windowExtendedState == Frame.NORMAL) {
        setLocation(TargetAWT.from(window.getLocation()));
        setSize(TargetAWT.from(window.getSize()));
      }
    }
  }

  private static boolean isFullScreen(@Nonnull Window window) {
    consulo.ui.Window uiWindow = TargetAWT.from(window);

    IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
    if (ideFrame != null && WindowManager.getInstance().isFullScreenSupportedInCurrentOS() && ideFrame.isInFullScreen()) {
      return true;
    }

    return false;
  }
}
