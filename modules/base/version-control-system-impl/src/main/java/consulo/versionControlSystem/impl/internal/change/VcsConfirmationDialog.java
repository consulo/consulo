/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.OptionsDialog;
import consulo.versionControlSystem.VcsShowConfirmationOption;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Dmitry Avdeev
 */
class VcsConfirmationDialog extends OptionsDialog {
    
    private final String myOkText;
    
    private final String myCancelText;
    private final VcsShowConfirmationOption myOption;
    private final String myMessage;
    private final String myDoNotShowMessage;

    VcsConfirmationDialog(Project project,
                          String title,
                          String okText,
                          String cancelText,
                          VcsShowConfirmationOption option,
                          String message,
                          String doNotShowMessage) {
        super(project);
        myOkText = okText;
        myCancelText = cancelText;
        myOption = option;
        myMessage = message;
        myDoNotShowMessage = doNotShowMessage;
        setTitle(title);
        init();
    }

    @Override
    protected boolean isToBeShown() {
        return myOption.getValue() == VcsShowConfirmationOption.Value.SHOW_CONFIRMATION;
    }

    @Override
    protected void setToBeShown(boolean value, boolean onOk) {
        myOption.setValue(value ? VcsShowConfirmationOption.Value.SHOW_CONFIRMATION : onOk ? VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY : VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY);
    }

    @Override
    protected boolean shouldSaveOptionsOnCancel() {
        return true;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.add(new JLabel(myMessage));
        panel.add(new JBLabel(Messages.getQuestionIcon()), BorderLayout.WEST);
        return panel;
    }

    
    @Override
    protected LocalizeValue getDoNotShowMessage() {
        return myDoNotShowMessage == null ? LocalizeValue.empty() : LocalizeValue.localizeTODO(myDoNotShowMessage);
    }

    
    @Override
    protected Action[] createActions() {
        AbstractAction okAction = new AbstractAction(myOkText) {
            {
                putValue(DEFAULT_ACTION, Boolean.TRUE);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                doOKAction();
            }
        };
        AbstractAction cancelAction = new AbstractAction(myCancelText) {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancelAction();
            }
        };
        return Platform.current().os().isMac() ? new Action[]{cancelAction, okAction} : new Action[]{okAction, cancelAction};
    }
}
