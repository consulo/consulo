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
package consulo.ui.ex.impl.internal;

import consulo.application.HelpManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;

public final class DialogHelpButton {
    private DialogHelpButton() {
    }

    @RequiredUIAccess
    public static Button create(DialogDescriptor descriptor) {
        Button button = Button.create(LocalizeValue.empty(), event -> {
            String helpId = descriptor.getHelpId();
            if (helpId != null) {
                HelpManager.getInstance().invokeHelp(helpId);
            }
        });
        button.addStyle(ButtonStyle.HELP);
        button.setToolTipText(ActionLocalize.actionHelptopicsDescription());
        return button;
    }
}
