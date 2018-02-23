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

import com.intellij.openapi.Disposable;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;
import consulo.annotations.RequiredDispatchThread;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public abstract class BaseWelcomeScreenPanel<E> extends JPanel {
  protected final JComponent myLeftComponent;
  protected final E myParam;

  @RequiredDispatchThread
  public BaseWelcomeScreenPanel(@Nonnull Disposable parentDisposable, @Nullable E param) {
    super(new BorderLayout());
    myParam = param;
    myLeftComponent = createLeftComponent(parentDisposable, param);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setBackground(WelcomeScreenConstants.getProjectsBackground());
    leftPanel.setBorder(new CustomLineBorder(WelcomeScreenConstants.getSeparatorColor(), JBUI.insetsRight(1)));
    leftPanel.setPreferredSize(JBUI.size(300, 460));
    leftPanel.add(myLeftComponent, BorderLayout.CENTER);

    add(leftPanel, BorderLayout.WEST);

    JComponent rightComponent = createRightComponent(param);

    add(rightComponent, BorderLayout.CENTER);
  }

  @Nonnull
  public JComponent getLeftComponent() {
    return myLeftComponent;
  }

  @Nonnull
  protected abstract JComponent createLeftComponent(@Nonnull Disposable parentDisposable, @Nullable E param);

  @Nonnull
  @RequiredDispatchThread
  protected abstract JComponent createRightComponent(@Nullable E param);
}
