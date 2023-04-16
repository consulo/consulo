// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.application.ui.impl;

import consulo.application.ui.WindowState;
import consulo.application.ui.WindowStateService;
import consulo.application.ui.impl.internal.BaseWindowStateBean;
import consulo.application.ui.impl.internal.UnifiedWindowStateServiceImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Coordinate2D;
import consulo.ui.Rectangle2D;
import consulo.ui.Size;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

abstract class DesktopAWTWindowStateServiceImpl extends UnifiedWindowStateServiceImpl<GraphicsConfiguration> {
  private static final Logger LOG = Logger.getInstance(WindowStateService.class);

  protected DesktopAWTWindowStateServiceImpl(@Nullable Project project) {
    super(project);
  }

  @Override
  protected BaseWindowStateBean newWindowStateBean() {
    return new WindowStateBean();
  }

  @Override
  public WindowState getStateFor(@Nullable Project project, @Nonnull String key, @Nonnull Window window) {
    synchronized (myRunnableMap) {
      WindowStateBean state = WindowStateAdapter.getState(window);
      Runnable runnable = myRunnableMap.put(key, new Runnable() {
        private long myModificationCount = state.getModificationCount();

        @Override
        public void run() {
          long newModificationCount = state.getModificationCount();
          if (myModificationCount != newModificationCount) {
            myModificationCount = newModificationCount;
            Coordinate2D location = state.getLocation();
            Size size = state.getSize();
            putFor(project, key, location, location != null, size, size != null, Frame.MAXIMIZED_BOTH == state.getExtendedState(), true, state.isFullScreen(), true);
          }
        }
      });
      if (runnable != null) {
        runnable.run();
      }
    }
    return getFor(project, key, WindowState.class);
  }

  @Override
  protected boolean isHeadless() {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance();
  }

  @Override
  @Nonnull
  protected String getAbsoluteKey(@Nullable GraphicsConfiguration configuration, @Nonnull String key) {
    StringBuilder sb = new StringBuilder(key);
    for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
      Rectangle bounds = ScreenUtil.getScreenRectangle(device.getDefaultConfiguration());
      sb.append('/').append(bounds.x);
      sb.append('.').append(bounds.y);
      sb.append('.').append(bounds.width);
      sb.append('.').append(bounds.height);
    }
    if (configuration != null) {
      Rectangle bounds = ScreenUtil.getScreenRectangle(configuration);
      sb.append('@').append(bounds.x);
      sb.append('.').append(bounds.y);
      sb.append('.').append(bounds.width);
      sb.append('.').append(bounds.height);
    }
    return sb.toString();
  }

  @Override
  @Nullable
  protected GraphicsConfiguration getConfiguration(@Nullable Object object) {
    if (object instanceof Project) {
      Project project = (Project)object;
      object = WindowManager.getInstance().getFrame(project);
      if (object == null) LOG.warn("cannot find a project frame for " + project);
    }
    if (object instanceof Window) {
      Window window = (Window)object;
      GraphicsConfiguration configuration = window.getGraphicsConfiguration();
      if (configuration != null) return configuration;
      object = ScreenUtil.getScreenDevice(window.getBounds());
      if (object == null) LOG.warn("cannot find a device for " + window);
    }
    if (object instanceof GraphicsDevice) {
      GraphicsDevice device = (GraphicsDevice)object;
      object = device.getDefaultConfiguration();
      if (object == null) LOG.warn("cannot find a configuration for " + device);
    }
    if (object instanceof GraphicsConfiguration) return (GraphicsConfiguration)object;
    if (object != null) LOG.warn("unexpected object " + object.getClass());
    return null;
  }

  @Override
  protected Rectangle2D getScreenRectangle(@Nonnull Coordinate2D location) {
    return TargetAWT.from(ScreenUtil.getScreenRectangle(location.getX(), location.getY()));
  }

  @Override
  @Nonnull
  protected Rectangle2D getScreenRectangle(@Nullable GraphicsConfiguration configuration) {
    Rectangle rectangle;
    if (configuration != null) {
      rectangle = ScreenUtil.getScreenRectangle(configuration);
    }
    else {
      rectangle = ScreenUtil.getMainScreenBounds();
    }

    return TargetAWT.from(rectangle);
  }

  @Override
  public boolean isVisible(Coordinate2D location, Size size) {
    if (location == null) {
      return size != null;
    }

    if (ScreenUtil.isVisible(new Point(location.getX(), location.getY()))) {
      return true;
    }
    if (size == null) {
      return false;
    }
    return ScreenUtil.isVisible(new Rectangle(location.getX(), location.getY(), size.getWidth(), size.getHeight()));
  }
}
