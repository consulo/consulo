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

package consulo.ui.ex.awt;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ReplacePromptDialog extends DialogWrapper {
    public enum PromptResult {
        OK, // 0
        CANCEL,
        SKIP,
        ALL,
        ALL_IN_THIS_FILE,
        ALL_FILES,
        SKIP_ALL_IN_THIS_FILE;

        private static final PromptResult[] VALUES = values();
    }

    private final boolean myIsMultiple;
    @Nullable
    private final Exception myException;

    public ReplacePromptDialog(boolean isMultipleFiles, LocalizeValue title, Project project) {
        this(isMultipleFiles, title, project, null);
    }

    public ReplacePromptDialog(boolean isMultipleFiles, LocalizeValue title, Project project, @Nullable Exception exception) {
        super(project, true);
        myIsMultiple = isMultipleFiles;
        myException = exception;
        setButtonsAlignment(SwingConstants.CENTER);
        setTitle(title);
        init();
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        DoAction replaceAction = new DoAction(UILocalize.replacePromptReplaceButton(), PromptResult.OK);
        replaceAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
        if (myException == null) {
            if (myIsMultiple) {
                return new Action[]{
                    replaceAction,
                    createSkipAction(),
                    new DoAction(UILocalize.replacePromptAllInThisFileButton(), PromptResult.ALL_IN_THIS_FILE),
                    new DoAction(UILocalize.replacePromptAllFilesAction(), PromptResult.ALL_FILES),
                    getCancelAction()
                };
            }
            else {
                return new Action[]{
                    replaceAction,
                    createSkipAction(),
                    new DoAction(UILocalize.replacePromptAllButton(), PromptResult.ALL),
                    getCancelAction()
                };
            }
        }
        else {
            return new Action[]{
                createSkipAction(),
                getCancelAction()
            };
        }
    }

    private DoAction createSkipAction() {
        return new DoAction(UILocalize.replacePromptSkipButton(), PromptResult.SKIP);
    }

    @Override
    public JComponent createNorthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        Image icon = Messages.getQuestionIcon();
        JLabel iconLabel = new JLabel(TargetAWT.to(icon));
        panel.add(iconLabel, BorderLayout.WEST);
        JLabel label = new JLabel(getMessage());
        label.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
        label.setForeground(JBColor.foreground());
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    protected String getMessage() {
        return myException == null ? UILocalize.replacePromptReplaceOccurrenceLabel().get() : myException.getMessage();
    }

    @Override
    public JComponent createCenterPanel() {
        return null;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "ReplaceDuplicatesPrompt";
    }

    @Nonnull
    public PromptResult getPromptResult() {
        return PromptResult.VALUES[getExitCode()];
    }

    private class DoAction extends LocalizeAction {
        private final PromptResult myExitCode;

        public DoAction(LocalizeValue text, PromptResult exitCode) {
            super(text);
            myExitCode = exitCode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            close(myExitCode.ordinal());
        }
    }
}

