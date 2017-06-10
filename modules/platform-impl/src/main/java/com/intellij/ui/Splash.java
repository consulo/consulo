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
package com.intellij.ui;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.JBUI;
import consulo.spash.AnimatedLogoLabel;
import consulo.util.SandboxUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;


/**
 * To customize your IDE splash go to YourIdeNameApplicationInfo.xml and find
 * section corresponding to IDE logo. It should look like:
 * <p>
 * &lt;logo url=&quot;/idea_logo.png&quot; textcolor=&quot;919191&quot; progressColor=&quot;264db5&quot; progressY=&quot;235&quot;/&gt;
 * </p>
 * <p>where <code>url</code> is path to your splash image
 * <p><code>textColor</code> is HEX representation of text color for user name
 * <p><code>progressColor</code> is progress bar color
 * <p><code>progressY</code> is Y coordinate of the progress bar
 * <p><code>progressTailIcon</code> is a path to flame effect icon
 *
 * @author Konstantin Bulenkov
 */
public class Splash extends JDialog implements StartupProgress {
  @Nullable
  public static Rectangle BOUNDS;

  private final AnimatedLogoLabel myLabel;

  /**
   * @param unstableScaling - cache scale value or always recalculate it.
   *                        On start - scaling may changed, and will provide some artifacts after LafManager loading
   */
  public Splash(boolean unstableScaling) {
    super((Frame)null, false);

    setUndecorated(true);
    if (!(SystemInfo.isLinux && SystemInfo.isJavaVersionAtLeast("1.7"))) {
      setResizable(false);
    }
    setFocusableWindowState(false);

    myLabel = new AnimatedLogoLabel(14, true, unstableScaling);
    myLabel.setForeground(SandboxUtil.isInsideSandbox() ? Color.WHITE : Color.BLACK);

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

  public void stopAnimation() {
    myLabel.stop();
  }

  @Override
  public void showProgress(String message, float progress) {
    myLabel.setValue((int)(progress * 100f));
  }
}
