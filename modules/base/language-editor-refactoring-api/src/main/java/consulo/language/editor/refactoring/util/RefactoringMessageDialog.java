/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.util;

import consulo.annotation.DeprecationInfo;
import consulo.application.HelpManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class RefactoringMessageDialog extends DialogWrapper {
    @Nonnull
    private final LocalizeValue myMessage;
    private final String myHelpTopic;
    private final Icon myIcon;
    private final boolean myIsCancelButtonVisible;

    public RefactoringMessageDialog(
        @Nonnull LocalizeValue title,
        @Nonnull LocalizeValue message,
        String helpTopic,
        Image icon,
        boolean showCancelButton,
        Project project
    ) {
        this(title, message, helpTopic, (Icon) icon, showCancelButton, project);
    }

    public RefactoringMessageDialog(
        @Nonnull LocalizeValue title,
        @Nonnull LocalizeValue message,
        String helpTopic,
        Icon icon,
        boolean showCancelButton,
        Project project
    ) {
        super(project, false);
        setTitle(title);
        myMessage = message;
        myHelpTopic = helpTopic;
        myIsCancelButtonVisible = showCancelButton;
        myIcon = icon;
        init();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public RefactoringMessageDialog(
        String title,
        String message,
        String helpTopic,
        String iconId,
        boolean showCancelButton,
        Project project
    ) {
        this(
            LocalizeValue.ofNullable(title),
            LocalizeValue.ofNullable(message),
            helpTopic,
            UIManager.getIcon(iconId),
            showCancelButton,
            project
        );
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        List<Action> actions = new ArrayList<>();
        actions.add(getOKAction());
        if (myIsCancelButtonVisible) {
            actions.add(getCancelAction());
        }
        if (myHelpTopic != null) {
            actions.add(getHelpAction());
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    protected JComponent createNorthPanel() {
        JLabel label = new JLabel(myMessage.get());
        label.setUI(new MultiLineLabelUI());

        JPanel panel = new JPanel(new BorderLayout(10, 0));
        if (myIcon != null) {
            panel.add(new JLabel(myIcon), BorderLayout.WEST);
            panel.add(label, BorderLayout.CENTER);
        }
        else {
            panel.add(label, BorderLayout.WEST);
        }
        return panel;
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    @Override
    @RequiredUIAccess
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(myHelpTopic);
    }
}
