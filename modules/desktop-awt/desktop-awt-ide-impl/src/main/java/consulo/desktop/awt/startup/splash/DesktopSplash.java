/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.desktop.awt.startup.splash;

import consulo.application.impl.internal.start.StartupProgress;
import consulo.application.util.SystemInfo;
import consulo.platform.Platform;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.application.ApplicationProperties;
import consulo.desktop.awt.ui.impl.window.JDialogAsUIWindow;

import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class DesktopSplash extends JDialogAsUIWindow implements StartupProgress {
  @Nullable
  public static Rectangle BOUNDS;

  private final AnimatedLogoLabel myLabel;

  /**
   * @param unstableScaling - cache scale value or always recalculate it.
   *                        On start - scaling may changed, and will provide some artifacts after LafManager loading
   */
  public DesktopSplash(boolean unstableScaling) {
    super(null, false);

    setUndecorated(true);
    if (!(Platform.current().os().isLinux() && SystemInfo.isJavaVersionAtLeast(7))) {
      setResizable(false);
    }
    setFocusableWindowState(false);

    myLabel = new AnimatedLogoLabel(14, true, unstableScaling);
    myLabel.setForeground(ApplicationProperties.isInSandbox() ? Color.WHITE : Color.BLACK);

    Container contentPane = getContentPane();
    contentPane.setBackground(Color.LIGHT_GRAY);
    contentPane.setPreferredSize(JBUI.size(602, 294));
    contentPane.setLayout(new BorderLayout());
    contentPane.add(myLabel, BorderLayout.SOUTH);
    Dimension size = getPreferredSize();
    setSize(size);
    pack();
    setLocationInTheCenterOfScreen();
  }

  private void setLocationInTheCenterOfScreen() {
    Rectangle bounds = getGraphicsConfiguration().getBounds();
    if (Platform.current().os().isWindows()) {
      Insets insets = ScreenUtil.getScreenInsets(getGraphicsConfiguration());
      int x = insets.left + (bounds.width - insets.left - insets.right - getWidth()) / 2;
      int y = insets.top + (bounds.height - insets.top - insets.bottom - getHeight()) / 2;
      setLocation(x, y);
    }
    else {
      setLocation((bounds.width - getWidth()) / 2, (bounds.height - getHeight()) / 2);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void show() {
    myLabel.start();

    super.show();
    toFront();
    BOUNDS = getBounds();
  }

  @Override
  public void dispose() {
    stopAnimation();
    myLabel.dispose();
    super.dispose();
  }

  public void stopAnimation() {
    myLabel.stop();
  }

  @Override
  public void showProgress(String message, float progress) {
    myLabel.setValue((int)(progress * 100f));
  }
}
