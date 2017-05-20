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
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public BaseWelcomeScreenPanel(@NotNull Disposable parentDisposable, @Nullable E param) {
    super(new BorderLayout());
    myParam = param;
    myLeftComponent = createLeftComponent(parentDisposable, param);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setBackground(FlatWelcomeFrame.getProjectsBackground());
    leftPanel.setBorder(new CustomLineBorder(FlatWelcomeFrame.getSeparatorColor(), JBUI.insetsRight(1)));
    leftPanel.setPreferredSize(JBUI.size(300, 460));
    leftPanel.add(myLeftComponent, BorderLayout.CENTER);

    add(leftPanel, BorderLayout.WEST);

    JComponent rightComponent = createRightComponent(param);

    add(rightComponent, BorderLayout.CENTER);
  }

  @NotNull
  public JComponent getLeftComponent() {
    return myLeftComponent;
  }

  @NotNull
  protected abstract JComponent createLeftComponent(@NotNull Disposable parentDisposable, @Nullable E param);

  @NotNull
  @RequiredDispatchThread
  protected abstract JComponent createRightComponent(@Nullable E param);
}
