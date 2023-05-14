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
package consulo.credentialStorage.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 23/01/2023
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface PasswordSafePromptDialog {
  /**
   * Ask password possibly asking password database first. The method could be invoked from any thread. If UI needs to be shown,
   * the method invokes {@link UIUtil#invokeAndWaitIfNeeded(Runnable)}
   *
   * @param project       the context project
   * @param title         the dialog title
   * @param message       the message describing a resource for which password is asked
   * @param requestor     the password requestor
   * @param key           the password key
   * @param resetPassword if true, the old password is removed from database and new password will be asked.
   * @param error         the error to show in the dialog       @return null if dialog was cancelled or password (stored in database or a entered by user)
   */
  @Nullable
  default String askPassword(final String title, final String message, @Nonnull final Class<?> requestor, final String key, boolean resetPassword, String error) {
    return askPassword(title, message, requestor, key, resetPassword, error, null, null);
  }

  /**
   * Ask password possibly asking password database first. The method could be invoked from any thread. If UI needs to be shown,
   * the method invokes {@link UIUtil#invokeAndWaitIfNeeded(Runnable)}
   *
   * @param title         the dialog title
   * @param message       the message describing a resource for which password is asked
   * @param requestor     the password requestor
   * @param key           the password key
   * @param resetPassword if true, the old password is removed from database and new password will be asked.
   * @return null if dialog was cancelled or password (stored in database or a entered by user)
   */
  @Nullable
  default String askPassword(final String title, final String message, @Nonnull final Class<?> requestor, final String key, boolean resetPassword) {
    return askPassword(title, message, requestor, key, resetPassword, null);
  }


  /**
   * Ask passphrase possibly asking password database first. The method could be invoked from any thread. If UI needs to be shown,
   * the method invokes {@link UIUtil#invokeAndWaitIfNeeded(Runnable)}
   *
   * @param project       the context project (might be null)
   * @param title         the dialog title
   * @param message       the message describing a resource for which password is asked
   * @param requestor     the password requestor
   * @param key           the password key
   * @param resetPassword if true, the old password is removed from database and new password will be asked.
   * @param error         the error to show in the dialog       @return null if dialog was cancelled or password (stored in database or a entered by user)
   */
  @Nullable
  default String askPassphrase(final String title, final String message, @Nonnull final Class<?> requestor, final String key, boolean resetPassword, String error) {
    return askPassword(title, message, requestor, key, resetPassword, error, "Passphrase:", "Remember the passphrase");
  }


  /**
   * Ask password possibly asking password database first. The method could be invoked from any thread. If UI needs to be shown,
   * the method invokes {@link UIUtil#invokeAndWaitIfNeeded(Runnable)}
   *
   * @param project       the context project
   * @param title         the dialog title
   * @param message       the message describing a resource for which password is asked
   * @param requestor     the password requestor
   * @param key           the password key
   * @param resetPassword if true, the old password is removed from database and new password will be asked.
   * @param error         the error text to show in the dialog
   * @param promptLabel   the prompt label text
   * @param checkboxLabel the checkbox text   @return null if dialog was cancelled or password (stored in database or a entered by user)
   */
  @Nullable
  String askPassword(final String title,
                     final String message,
                     @Nonnull final Class<?> requestor,
                     final String key,
                     boolean resetPassword,
                     final String error,
                     final String promptLabel,
                     final String checkboxLabel);
}
