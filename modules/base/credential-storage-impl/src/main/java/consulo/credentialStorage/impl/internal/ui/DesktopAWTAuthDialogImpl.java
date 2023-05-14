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
import consulo.credentialStorage.ui.AuthDialog;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 28/01/2023
 */
@Singleton
@ServiceImpl
public class DesktopAWTAuthDialogImpl implements AuthDialog {
  private final Project myProject;

  @Inject
  public DesktopAWTAuthDialogImpl(Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public AuthenticationData show(@Nonnull String title,
                                 @Nullable String description,
                                 @Nullable String login,
                                 @Nullable String password,
                                 boolean rememberByDefault) {
    AuthDialogImpl dialog = new AuthDialogImpl(myProject, title, description, login, password, rememberByDefault);
    dialog.show();
    if (dialog.isOK()) {
      return new AuthenticationData(dialog.getUsername(), dialog.getPassword(), dialog.isRememberPassword());
    }
    return null;
  }
}
