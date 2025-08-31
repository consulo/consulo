/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.credentialStorage.impl.internal.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.MnemonicHelper;
import consulo.ui.ex.awt.DialogWrapper;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class AuthenticationDialogImpl extends DialogWrapper {
  private final AuthenticationPanel panel;

  public AuthenticationDialogImpl(@Nonnull Component component, String title, String description, String login, String password, boolean rememberPassword) {
    super(component, true);
    setTitle(title);

    MnemonicHelper.init(getContentPane());
    panel = new AuthenticationPanel(description, login, password, rememberPassword);

    Window window = getWindow();
    if (window instanceof JDialog) {
      ((JDialog) window).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    init();
  }

  public AuthenticationDialogImpl(String title, String description, String login, String password, boolean rememberPassword) {
    super(JOptionPane.getRootFrame(), true);
    setTitle(title);

    MnemonicHelper.init(getContentPane());
    panel = new AuthenticationPanel(description, login, password, rememberPassword);

    Window window = getWindow();
    if (window instanceof JDialog) {
      ((JDialog) window).setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    init();
  }

  public String getLogin() {
    return panel.getLogin();
  }

  public char[] getPassword() {
    return panel.getPassword();
  }

  public boolean isRememberPassword() {
    return panel.isRememberPassword();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return panel.getPreferredFocusedComponent();
  }

  @Override
  @Nullable
  protected JComponent createCenterPanel() {
    return panel;
  }
}
