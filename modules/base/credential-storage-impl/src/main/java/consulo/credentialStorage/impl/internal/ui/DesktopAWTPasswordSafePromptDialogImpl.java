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
import consulo.credentialStorage.ui.PasswordSafePromptDialog;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23/01/2023
 */
@Singleton
@ServiceImpl
public class DesktopAWTPasswordSafePromptDialogImpl implements PasswordSafePromptDialog {
  private final Project myProject;

  @Inject
  public DesktopAWTPasswordSafePromptDialogImpl(Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public String askPassword(String title, String message, @Nonnull Class<?> requestor, String key, boolean resetPassword, String error, String promptLabel, String checkboxLabel) {
    return PasswordSafePromptDialogImpl.askPassword(myProject, title, message, requestor, key, resetPassword, error, promptLabel, checkboxLabel);
  }
}
