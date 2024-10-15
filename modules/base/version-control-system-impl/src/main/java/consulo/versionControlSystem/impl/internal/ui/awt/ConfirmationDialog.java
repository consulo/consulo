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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.OptionsMessageDialog;
import consulo.ui.image.Image;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ConfirmationDialog extends OptionsMessageDialog {
    private final VcsShowConfirmationOption myOption;
    private LocalizeValue myDoNotShowAgainMessage;
    private final LocalizeValue myOkActionName;
    private final LocalizeValue myCancelActionName;

    @RequiredUIAccess
    public static boolean requestForConfirmation(
        @Nonnull VcsShowConfirmationOption option,
        @Nonnull Project project,
        @Nonnull LocalizeValue message,
        @Nonnull LocalizeValue title,
        @Nullable Image icon
    ) {
        return requestForConfirmation(option, project, message, title, icon, CommonLocalize.buttonYes(), CommonLocalize.buttonNo());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
    public static boolean requestForConfirmation(
        @Nonnull VcsShowConfirmationOption option,
        @Nonnull Project project,
        @Nonnull String message,
        @Nonnull String title,
        @Nullable Image icon
    ) {
        return requestForConfirmation(
            option,
            project,
            message,
            title,
            icon,
            CommonLocalize.buttonYes().get(),
            CommonLocalize.buttonNo().get()
        );
    }

    @RequiredUIAccess
    public static boolean requestForConfirmation(
        @Nonnull VcsShowConfirmationOption option,
        @Nonnull Project project,
        @Nonnull LocalizeValue message,
        @Nonnull LocalizeValue title,
        @Nullable Image icon,
        @Nonnull LocalizeValue okActionName,
        @Nonnull LocalizeValue cancelActionName
    ) {
        if (option.getValue() == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
            return false;
        }
        final ConfirmationDialog dialog = new ConfirmationDialog(project, message, title, icon, option, okActionName, cancelActionName);
        if (!option.isPersistent()) {
            dialog.setDoNotAskOption(null);
        }
        else {
            dialog.setDoNotShowAgainMessage(CommonLocalize.dialogOptionsDoNotAsk());
        }
        return dialog.showAndGet();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
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
            dialog.setDoNotShowAgainMessage(CommonLocalize.dialogOptionsDoNotAsk());
        }
        return dialog.showAndGet();
    }

    public ConfirmationDialog(
        Project project,
        @Nonnull LocalizeValue message,
        @Nonnull LocalizeValue title,
        final Image icon,
        final VcsShowConfirmationOption option
    ) {
        this(project, message, title, icon, option, CommonLocalize.buttonYes(), CommonLocalize.buttonNo());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ConfirmationDialog(
        Project project,
        final String message,
        String title,
        final Image icon,
        final VcsShowConfirmationOption option
    ) {
        this(project, message, title, icon, option, CommonLocalize.buttonYes().get(), CommonLocalize.buttonNo().get());
    }

    public ConfirmationDialog(
        Project project,
        @Nonnull LocalizeValue message,
        @Nonnull LocalizeValue title,
        final Image icon,
        final VcsShowConfirmationOption option,
        @Nonnull LocalizeValue okActionName,
        @Nonnull LocalizeValue cancelActionName
    ) {
        super(project, message, title, icon);
        myOption = option;
        myOkActionName = okActionName;
        myCancelActionName = cancelActionName;
        init();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
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
        myOkActionName = okActionName != null ? LocalizeValue.of(okActionName) : CommonLocalize.buttonYes();
        myCancelActionName = cancelActionName != null ? LocalizeValue.of(cancelActionName) : CommonLocalize.buttonNo();
        init();
    }

    public void setDoNotShowAgainMessage(@Nonnull LocalizeValue doNotShowAgainMessage) {
        myDoNotShowAgainMessage = doNotShowAgainMessage;
        myCheckBoxDoNotShowDialog.setText(doNotShowAgainMessage.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void setDoNotShowAgainMessage(final String doNotShowAgainMessage) {
        setDoNotShowAgainMessage(LocalizeValue.ofNullable(doNotShowAgainMessage));
    }

    @Nonnull
    //TODO: rename to getDoNotShowMessage() after deprecation removal
    protected LocalizeValue getDoNotShowMessageValue() {
        return myDoNotShowAgainMessage;
    }

    @Deprecated
    @DeprecationInfo("Use #getDoNotShowMessageValue()")
    @Nonnull
    @Override
    protected String getDoNotShowMessage() {
        return myDoNotShowAgainMessage == LocalizeValue.empty() ? super.getDoNotShowMessage() : myDoNotShowAgainMessage.get();
    }

    @Nonnull
    @Override
    //TODO: rename to getOkActionName() after deprecation removal
    public LocalizeValue getOkActionValue() {
        return myOkActionName;
    }

    @Nonnull
    @Override
    //TODO: rename to getCancelActionName() after deprecation removal
    public LocalizeValue getCancelActionValue() {
        return myCancelActionName;
    }

    @Override
    protected String getOkActionName() {
        return myOkActionName.get();
    }

    @Override
    protected String getCancelActionName() {
        return myCancelActionName.get();
    }

    @Override
    protected boolean isToBeShown() {
        return myOption.getValue() == VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
    }

    @Override
    protected void setToBeShown(boolean value, boolean onOk) {
        final VcsShowConfirmationOption.Value optionValue = value
            ? VcsShowConfirmationOption.Value.SHOW_CONFIRMATION
            : onOk
            ? VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY
            : VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY;

        myOption.setValue(optionValue);
    }

    @Override
    protected boolean shouldSaveOptionsOnCancel() {
        return true;
    }
}
