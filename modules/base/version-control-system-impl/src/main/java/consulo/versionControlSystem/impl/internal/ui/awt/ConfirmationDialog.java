/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.ui.awt;

import consulo.application.CommonBundle;
import consulo.project.Project;
import consulo.ui.ex.awt.OptionsMessageDialog;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ConfirmationDialog extends OptionsMessageDialog {
    private final VcsShowConfirmationOption myOption;
    private String myDoNotShowAgainMessage;
    private final String myOkActionName;
    private final String myCancelActionName;

    public static boolean requestForConfirmation(
        @Nonnull VcsShowConfirmationOption option,
        @Nonnull Project project,
        @Nonnull String message,
        @Nonnull String title,
        @Nullable Image icon
    ) {
        return requestForConfirmation(option, project, message, title, icon, null, null);
    }

    public static boolean requestForConfirmation(
        @Nonnull VcsShowConfirmationOption option,
        @Nonnull Project project,
        @Nonnull String message,
        @Nonnull String title,
        @Nullable Image icon,
        @Nullable String okActionName,
        @Nullable String cancelActionName
    ) {
        if (option.getValue() == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
            return false;
        }
        final ConfirmationDialog dialog = new ConfirmationDialog(project, message, title, icon, option, okActionName, cancelActionName);
        if (!option.isPersistent()) {
            dialog.setDoNotAskOption(null);
        }
        else {
            dialog.setDoNotShowAgainMessage(CommonBundle.message("dialog.options.do.not.ask"));
        }
        return dialog.showAndGet();
    }

    public ConfirmationDialog(
        Project project,
        final String message,
        String title,
        final Image icon,
        final VcsShowConfirmationOption option
    ) {
        this(project, message, title, icon, option, null, null);
    }

    public ConfirmationDialog(
        Project project,
        final String message,
        String title,
        final Image icon,
        final VcsShowConfirmationOption option,
        @Nullable String okActionName,
        @Nullable String cancelActionName
    ) {
        super(project, message, title, icon);
        myOption = option;
        myOkActionName = okActionName != null ? okActionName : CommonBundle.getYesButtonText();
        myCancelActionName = cancelActionName != null ? cancelActionName : CommonBundle.getNoButtonText();
        init();
    }

    public void setDoNotShowAgainMessage(final String doNotShowAgainMessage) {
        myDoNotShowAgainMessage = doNotShowAgainMessage;
        myCheckBoxDoNotShowDialog.setText(doNotShowAgainMessage);
    }

    @Nonnull
    @Override
    protected String getDoNotShowMessage() {
        return myDoNotShowAgainMessage == null ? super.getDoNotShowMessage() : myDoNotShowAgainMessage;
    }

    @Override
    protected String getOkActionName() {
        return myOkActionName;
    }

    @Override
    protected String getCancelActionName() {
        return myCancelActionName;
    }

    @Override
    protected boolean isToBeShown() {
        return myOption.getValue() == VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
    }

    @Override
    protected void setToBeShown(boolean value, boolean onOk) {
        final VcsShowConfirmationOption.Value optionValue;

        if (value) {
            optionValue = VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
        }
        else if (onOk) {
            optionValue = VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY;
        }
        else {
            optionValue = VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY;
        }

        myOption.setValue(optionValue);
    }

    @Override
    protected boolean shouldSaveOptionsOnCancel() {
        return true;
    }
}
