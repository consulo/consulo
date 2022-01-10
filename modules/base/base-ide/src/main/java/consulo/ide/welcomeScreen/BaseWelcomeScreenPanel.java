/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.welcomeScreen;

import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public abstract class BaseWelcomeScreenPanel extends JPanel {
  protected final JComponent myLeftComponent;

  @RequiredUIAccess
  public BaseWelcomeScreenPanel(@Nonnull Disposable parentDisposable) {
    super(new BorderLayout());
    myLeftComponent = createLeftComponent(parentDisposable);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setBorder(new CustomLineBorder(UIUtil.getBorderColor(), JBUI.insetsRight(1)));
    leftPanel.setPreferredSize(JBUI.size(getLeftComponentWidth(), -1));
    leftPanel.add(myLeftComponent, BorderLayout.CENTER);

    add(leftPanel, BorderLayout.WEST);

    JComponent rightComponent = createRightComponent();

    add(rightComponent, BorderLayout.CENTER);
  }

  protected int getLeftComponentWidth() {
    return 400;
  }

  @Nonnull
  public JComponent getLeftComponent() {
    return myLeftComponent;
  }

  @Nonnull
  protected abstract JComponent createLeftComponent(@Nonnull Disposable parentDisposable);

  @Nonnull
  @RequiredUIAccess
  protected abstract JComponent createRightComponent();
}
