/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

/**
 * A dialog around a change list name: the reason the name can not be used is shown under the content and keeps OK
 * disabled.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public abstract class ChangeListDialogDescriptor extends DialogDescriptor {
    private LocalizeValue myErrorText = LocalizeValue.empty();
    private @Nullable Label myErrorLabel;

    protected ChangeListDialogDescriptor(LocalizeValue title) {
        super(title);
    }

    @RequiredUIAccess
    protected final void setErrorText(LocalizeValue errorText) {
        myErrorText = errorText;
        Label errorLabel = myErrorLabel;
        if (errorLabel != null) {
            errorLabel.setText(errorText);
            errorLabel.setVisible(!errorText.isEmpty());
        }
        updateOkButtonState();
    }

    @RequiredUIAccess
    protected final Component withErrorLabel(Component content) {
        Label errorLabel = Label.create(myErrorText);
        errorLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        errorLabel.setVisible(!myErrorText.isEmpty());
        myErrorLabel = errorLabel;

        DockLayout layout = DockLayout.create();
        layout.center(content);
        layout.bottom(errorLabel);
        return layout;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myErrorText.isEmpty();
    }
}
