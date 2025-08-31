// Copyright 2008-2010 Victor Iacoban
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under
// the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.
package consulo.credentialStorage.impl.internal.ui;

import consulo.credentialStorage.PasswordSafe;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class AuthDialogImpl extends DialogWrapper {
  private AuthenticationPanel authPanel;

  /**
   * If password if prefilled, it is expected to continue remembering it.
   * On the other hand, if password saving is disabled, the checkbox is not shown.
   * In other cases, {@code rememberByDefault} is used.
   */
  public AuthDialogImpl(@Nonnull Project project, @Nonnull String title, @Nullable String description, @Nullable String login, @Nullable String password, boolean rememberByDefault) {
    super(project, false);
    setTitle(title);
    boolean rememberPassword = decideOnShowRememberPasswordOption(password, rememberByDefault);
    authPanel = new AuthenticationPanel(description, login, password, rememberPassword);
    init();
  }

  private static boolean decideOnShowRememberPasswordOption(@Nullable String password, boolean rememberByDefault) {
    PasswordSafe passwordSafe = PasswordSafe.getInstance();
    // if password saving is disabled, don't show the checkbox.
    if (passwordSafe.isMemoryOnly()) {
      return false;
    }
    // if password is prefilled, it is expected to continue remembering it.
    if (!StringUtil.isEmptyOrSpaces(password)) {
      return true;
    }
    return rememberByDefault;
  }

  protected JComponent createCenterPanel() {
    return authPanel;
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return authPanel.getPreferredFocusedComponent();
  }

  public String getUsername() {
    return authPanel.getLogin();
  }

  public char[] getPassword() {
    return authPanel.getPassword();
  }

  public boolean isRememberPassword() {
    return authPanel.isRememberPassword();
  }
}
