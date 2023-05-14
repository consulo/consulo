/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.taskBar.DefaultJava9TaskBarImpl;
import consulo.desktop.awt.ui.impl.taskBar.MacTaskBarImpl;
import consulo.desktop.awt.ui.impl.taskBar.Windows7TaskBarImpl;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.ui.TaskBar;
import consulo.ui.Window;
import consulo.ui.impl.DummyTaskBarImpl;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
public class DesktopTaskBarImpl implements TaskBar {
  public static final DesktopTaskBarImpl ourInstance = new DesktopTaskBarImpl();

  private final TaskBar myDelegate;

  private DesktopTaskBarImpl() {
    PlatformOperatingSystem operatingSystem = Platform.current().os();

    if(Taskbar.isTaskbarSupported()) {
      if (operatingSystem.isWindows() && operatingSystem.asWindows().isWindows7OrNewer()) {
        myDelegate = new Windows7TaskBarImpl();
      }
      else if (operatingSystem.isMac()) {
        myDelegate = new MacTaskBarImpl();
      }
      else {
        myDelegate = new DefaultJava9TaskBarImpl();
      }
    }
    else {
      myDelegate = new DummyTaskBarImpl();
    }
  }

  @Override
  public void requestFocus(@Nonnull Window window) {
    myDelegate.requestFocus(window);
  }

  @Override
  public boolean setProgress(@Nonnull Window window, Object processId, ProgressScheme scheme, double value, boolean isOk) {
    return myDelegate.setProgress(window, processId, scheme, value, isOk);
  }

  @Override
  public boolean hideProgress(@Nonnull Window window, Object processId) {
    return myDelegate.hideProgress(window, processId);
  }

  @Override
  public void setTextBadge(@Nonnull Window window, String text) {
    myDelegate.setTextBadge(window, text);
  }

  @Override
  public void setErrorBadge(@Nonnull Window window, String text) {
    myDelegate.setErrorBadge(window, text);
  }

  @Override
  public void setOkBadge(@Nonnull Window window, boolean visible) {
    myDelegate.setOkBadge(window, visible);
  }

  @Override
  public void requestAttention(@Nonnull Window window, boolean critical) {
    myDelegate.requestAttention(window, critical);
  }
}

