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
package consulo.ui.desktop.internal.taskBar;

import com.intellij.util.ui.ImageUtil;
import consulo.awt.TargetAWT;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TaskBar;
import consulo.ui.Window;
import consulo.ui.image.ImageKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
public class DefaultJava9TaskBarImpl implements TaskBar {
  // TODO [VISTALL] impl via java.desktop API. but not all methods are supported

  protected Taskbar myTaskbar;
  private Desktop myDesktop;

  protected Object myCurrentProcessId;
  protected double myLastValue;
  private BufferedImage myOkImage;

  public DefaultJava9TaskBarImpl() {
    myTaskbar = Taskbar.getTaskbar();
  }

  @Override
  public boolean setProgress(@Nonnull Window window, Object processId, ProgressScheme scheme, double value, boolean isOk) {
    myCurrentProcessId = processId;

    if (Math.abs(myLastValue - value) < 0.02d) {
      return true;
    }

    if (myTaskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE_WINDOW)) {
      myTaskbar.setWindowProgressValue(TargetAWT.to(window), (int)(value * 100.));
    }
    else if (myTaskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
      myTaskbar.setProgressValue((int)(value * 100.));
    }

    myLastValue = value;
    myCurrentProcessId = null;
    return true;
  }

  @Override
  public boolean hideProgress(@Nonnull Window window, Object processId) {
    if (myCurrentProcessId != null && !myCurrentProcessId.equals(processId)) {
      return false;
    }

    if (myTaskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE_WINDOW)) {
      myTaskbar.setWindowProgressValue(TargetAWT.to(window), -1);
    }
    else if (myTaskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
      myTaskbar.setProgressValue(-1);
    }

    myCurrentProcessId = null;
    myLastValue = 0;
    return true;
  }

  @Override
  public final void requestAttention(@Nonnull Window window, boolean critical) {
    if (myTaskbar.isSupported(Taskbar.Feature.USER_ATTENTION)) {
      myTaskbar.requestUserAttention(true, critical);
    }
  }

  @Override
  public final void requestFocus(@Nonnull Window window) {
    if(myTaskbar.isSupported(Taskbar.Feature.USER_ATTENTION_WINDOW)) {
      myTaskbar.requestWindowUserAttention(TargetAWT.to(window));
    }
  }

  @Override
  public final void setTextBadge(@Nonnull Window window, String text) {
    if (!isValid(window)) {
      return;
    }

    if (myTaskbar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)) {
      myTaskbar.setIconBadge(text);
    }
    else {
      setTextBadgeUnsupported(window, text);
    }
  }

  protected void setTextBadgeUnsupported(@Nonnull Window window, @Nullable String text) {
  }

  @Override
  public void setOkBadge(@Nonnull Window window, boolean visible) {
    if (!isValid(window)) {
      return;
    }

    if (myTaskbar.isSupported(Taskbar.Feature.ICON_BADGE_IMAGE_WINDOW)) {
      BufferedImage icon = null;
      if (visible) {
        if (myOkImage == null) {
          ImageKey okIcon = PlatformIconGroup.macAppIconOk512();
          Image imageKeyImage = TargetAWT.toImage(okIcon, null);
          myOkImage = ImageUtil.toBufferedImage(imageKeyImage);
        }

        icon = myOkImage;
      }

      myTaskbar.setWindowIconBadge(TargetAWT.to(window), icon);
    }
    else {
      setOkBadgeUnsupported(window, visible);
    }
  }

  protected void setOkBadgeUnsupported(@Nonnull Window window, boolean visible) {
  }

  private static boolean isValid(@Nullable Window window) {
    if (window == null) {
      return false;
    }

    java.awt.Window awtWindow = TargetAWT.to(window);
    return awtWindow != null && awtWindow.isDisplayable();
  }
}
