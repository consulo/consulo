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
package com.intellij.vcsUtil;

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.net.AuthenticationPanel;
import consulo.annotations.RequiredDispatchThread;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

public class AuthDialog extends DialogWrapper {
  private AuthenticationPanel authPanel;

  /**
   * If password if prefilled, it is expected to continue remembering it.
   * On the other hand, if password saving is disabled, the checkbox is not shown.
   * In other cases, {@code rememberByDefault} is used.
   */
  public AuthDialog(@Nonnull Project project, @Nonnull String title, @Nullable String description, @Nullable String login, @Nullable String password, boolean rememberByDefault) {
    super(project, false);
    setTitle(title);
    Boolean rememberPassword = decideOnShowRememberPasswordOption(password, rememberByDefault);
    authPanel = new AuthenticationPanel(description, login, password, rememberPassword);
    init();
  }

  private static Boolean decideOnShowRememberPasswordOption(@Nullable String password, boolean rememberByDefault) {
    // if password saving is disabled, don't show the checkbox.
    if (PasswordSafe.getInstance().isMemoryOnly()) {
      return null;
    }
    // if password is prefilled, it is expected to continue remembering it.
    if (!StringUtil.isEmptyOrSpaces(password)) {
      return true;
    }
    return rememberByDefault;
  }

  @Override
  protected JComponent createCenterPanel() {
    return authPanel;
  }

  @RequiredDispatchThread
  @Override
  public JComponent getPreferredFocusedComponent() {
    return authPanel.getPreferredFocusedComponent();
  }

  public String getUsername() {
    return authPanel.getLogin();
  }

  public String getPassword() {
    return String.valueOf(authPanel.getPassword());
  }

  public boolean isRememberPassword() {
    return authPanel.isRememberPassword();
  }
  
}
