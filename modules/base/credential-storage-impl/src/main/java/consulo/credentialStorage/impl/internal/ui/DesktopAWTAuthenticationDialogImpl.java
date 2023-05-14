/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.credentialStorage.AuthenticationData;
import consulo.credentialStorage.ui.AuthenticationDialog;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.util.PopupUtil;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23/01/2023
 */
@Singleton
@ServiceImpl
public class DesktopAWTAuthenticationDialogImpl implements AuthenticationDialog {
  @Nullable
  @Override
  public AuthenticationData showNoSafe(String title, String description, String login, String password, boolean rememberPassword) {
    AuthenticationDialogImpl dialog = new AuthenticationDialogImpl(PopupUtil.getActiveComponent(), title, description, login, password, rememberPassword);
    dialog.show();
    if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
      return new AuthenticationData(dialog.getLogin(), dialog.getPassword(), dialog.isRememberPassword());
    }
    return null;
  }
}
