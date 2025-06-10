// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.application.ui.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.impl.internal.UnifiedDimensionServiceImpl;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Point2D;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;

/**
 * This class represents map between strings and rectangles. It's intended to store
 * sizes of window, dialogs, etc.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class DesktopAWTDimensionServiceImpl extends UnifiedDimensionServiceImpl {
  @Override
  protected boolean isOutVisibleScreenArea(Point2D point2D) {
    Point point = new Point(point2D.x(), point2D.y());
    return !ScreenUtil.getScreenRectangle(point).contains(point);
  }

  /**
   * @return Pair(key, scale) where:
   * key is the HiDPI-aware key,
   * scale is the HiDPI-aware factor to transform size metrics.
   */
  @Override
  @Nonnull
  protected Pair<String, Float> resolveScale(String key, @Nullable Project project) {
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    if (env.isHeadlessInstance()) {
      return new Pair<>(key + ".headless", 1f);
    }

    consulo.ui.Window uiWindow = null;
    final Component owner = IdeFocusManager.findInstance().getFocusOwner();
    if (owner != null) {
      uiWindow = TargetAWT.from(UIUtil.getParentOfType(JFrame.class, owner));
    }
    if (uiWindow == null) {
      uiWindow = WindowManager.getInstance().findVisibleWindow();
    }

    if (project != null && (uiWindow == null || (uiWindow.getUserData(IdeFrame.KEY) != null && project != uiWindow.getUserData(IdeFrame.KEY).getProject()))) {
      uiWindow = WindowManager.getInstance().getWindow(project);
    }

    Rectangle screen = new Rectangle(0, 0, 0, 0);
    GraphicsDevice gd = null;
    if (uiWindow != null) {
      Window awtWindow = TargetAWT.to(uiWindow);
      final Point topLeft = awtWindow.getLocation();
      Point center = new Point(topLeft.x + awtWindow.getWidth() / 2, topLeft.y + awtWindow.getHeight() / 2);
      for (GraphicsDevice device : env.getScreenDevices()) {
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        if (bounds.contains(center)) {
          screen = bounds;
          gd = device;
          break;
        }
      }
    }
    if (gd == null) {
      gd = env.getDefaultScreenDevice();
      screen = gd.getDefaultConfiguration().getBounds();
    }
    float scale = 1f;
    if (UIUtil.isJreHiDPIEnabled()) {
      scale = JBUIScale.sysScale(gd.getDefaultConfiguration());
      // normalize screen bounds
      screen.setBounds((int)Math.floor(screen.x * scale), (int)Math.floor(screen.y * scale), (int)Math.ceil(screen.width * scale), (int)Math.ceil(screen.height * scale));
    }
    String realKey = key + '.' + screen.x + '.' + screen.y + '.' + screen.width + '.' + screen.height;
    if (JBUI.isPixHiDPI(gd.getDefaultConfiguration())) {
      int dpi = ((int)(96 * JBUI.pixScale(gd.getDefaultConfiguration())));
      realKey += "@" + dpi + "dpi";
    }
    return new Pair<>(realKey, scale);
  }
}
