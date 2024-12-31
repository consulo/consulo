/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.ex.dialog.action;

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 13/12/2021
 */
public class DialogCancelAction extends DumbAwareAction {
    public DialogCancelAction() {
        super(CommonLocalize.buttonCancel(), LocalizeValue.of(), null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Dialog data = e.getRequiredData(Dialog.KEY);

        DialogDescriptor descriptor = data.getDescriptor();
        if (descriptor.canHandle(this, null)) {
            descriptor.onHandleValue(this, null);
            
            data.doCancelAction();
        }
    }
}
