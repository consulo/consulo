/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.credentialStorage.PasswordSafe;
import consulo.credentialStorage.impl.internal.PasswordSafeImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * The generic password dialog. Use it to ask a password from user with option to remember it.
 */
public class PasswordSafePromptDialogImpl extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(PasswordSafePromptDialogImpl.class);

    private final PasswordPromptComponent myComponent;

    /**
     * The private constructor. Note that it does not do init on dialog.
     *
     * @param project the project
     * @param title   the dialog title
     * @param message the message on the dialog
     * @param type
     */
    private PasswordSafePromptDialogImpl(@Nullable Project project, @Nonnull String title, @Nonnull PasswordPromptComponent component) {
        super(project, true);
        setTitle(title);
        myComponent = component;
        setResizable(false);
        init();
    }

    public PasswordPromptComponent getComponent() {
        return myComponent;
    }

    @Override
    protected JComponent createCenterPanel() {
        return myComponent.getComponent();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myComponent.getPreferredFocusedComponent();
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
     * @param error         the error to show in the dialog       @return null if dialog was cancelled or password (stored in database or a entered by user)
     */
    @Nullable
    public static String askPassword(final Project project, final String title, final String message, @Nonnull final Class<?> requestor, final String key, boolean resetPassword, String error) {
        return askPassword(project, title, message, requestor, key, resetPassword, error, null, null);
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
    public static String askPassword(final String title, final String message, @Nonnull final Class<?> requestor, final String key, boolean resetPassword) {
        return askPassword(null, title, message, requestor, key, resetPassword, null);
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
    public static String askPassphrase(final Project project, final String title, final String message, @Nonnull final Class<?> requestor, final String key, boolean resetPassword, String error) {
        return askPassword(project, title, message, requestor, key, resetPassword, error, "Passphrase:", "Remember the passphrase");
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
    @RequiredUIAccess
    static String askPassword(final Project project,
                              final String title,
                              final String message,
                              @Nonnull final Class<?> requestor,
                              final String key,
                              boolean resetPassword,
                              final String error,
                              final String promptLabel,
                              final String checkboxLabel) {
        final PasswordSafeImpl ps = (PasswordSafeImpl) PasswordSafe.getInstance();

        if (resetPassword) {
            ps.storePassword(project, requestor, key, null);
        }
        else {
            String pw = ps.getPassword(project, requestor, key);
            if (pw != null) {
                return pw;
            }
        }

        Ref<String> ref = Ref.create();

        Application application = Application.get();

        application.invokeAndWait(() -> {
            boolean memoryOnly = ps.isMemoryOnly();
            final PasswordPromptComponent component = new PasswordPromptComponent(memoryOnly, message, false, promptLabel, checkboxLabel);
            PasswordSafePromptDialogImpl d = new PasswordSafePromptDialogImpl(project, title, component);

            d.setErrorText(error);
            if (d.showAndGet()) {
                ref.set(new String(component.getPassword()));

                if (component.isRememberSelected() || memoryOnly) {
                    ps.storePassword(project, requestor, key, ref.get());
                }
            }
        }, application.getAnyModalityState());
        return ref.get();
    }
}

