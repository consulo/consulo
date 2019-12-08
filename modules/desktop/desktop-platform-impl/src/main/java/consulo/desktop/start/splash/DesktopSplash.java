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
package consulo.desktop.start.splash;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.ScreenUtil;
import com.intellij.util.ui.JBUI;
import consulo.application.ApplicationProperties;
import consulo.ui.desktop.internal.window.JDialogAsUIWindow;

import javax.annotation.Nullable;
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
    if (!(SystemInfo.isLinux && SystemInfo.isJavaVersionAtLeast(7))) {
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
    if (SystemInfo.isWindows) {
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
