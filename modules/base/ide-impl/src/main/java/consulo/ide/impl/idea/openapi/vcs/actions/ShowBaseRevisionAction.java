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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.DateFormatUtil;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.ide.impl.idea.openapi.vcs.changes.BackgroundFromStartOption;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.history.VcsRevisionDescription;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Irina.Chernushina
 * @since 2011-08-02
 */
@ActionImpl(id = "Show.Current.Revision")
public class ShowBaseRevisionAction extends AbstractVcsAction {
    public ShowBaseRevisionAction() {
        super(LocalizeValue.localizeTODO("Show Current Revision"));
    }

    @Override
    @RequiredUIAccess
    protected void actionPerformed(@Nonnull VcsContext vcsContext) {
        AbstractVcs vcs = AbstractShowDiffAction.isEnabled(vcsContext, null);
        if (vcs == null) {
            return;
        }
        VirtualFile[] selectedFilePaths = vcsContext.getSelectedFiles();
        if (selectedFilePaths == null || selectedFilePaths.length != 1) {
            return;
        }
        VirtualFile selectedFile = selectedFilePaths[0];
        if (selectedFile.isDirectory()) {
            return;
        }

        ProgressManager.getInstance().run(new MyTask(selectedFile, vcs, vcsContext));
    }

    private static class MyTask extends Task.Backgroundable {
        private final AbstractVcs vcs;
        private final VirtualFile selectedFile;
        private VcsRevisionDescription myDescription;
        private VcsContext vcsContext;

        private MyTask(VirtualFile selectedFile, AbstractVcs vcs, VcsContext vcsContext) {
            super(
                vcsContext.getProject(),
                LocalizeValue.localizeTODO("Loading current revision"),
                true,
                BackgroundFromStartOption.getInstance()
            );
            this.selectedFile = selectedFile;
            this.vcs = vcs;
            this.vcsContext = vcsContext;
        }

        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
            DiffProvider diffProvider = vcs.getDiffProvider();
            if (diffProvider == null) {
                return;
            }

            myDescription = diffProvider.getCurrentRevisionDescription(selectedFile);
        }

        @RequiredUIAccess
        @Override
        public void onSuccess() {
            if (myProject.isDisposed() || !((Project) myProject).isOpen()) {
                return;
            }

            if (myDescription != null) {
                NotificationPanel panel = new NotificationPanel();
                panel.setText(createMessage(myDescription, selectedFile));
                JBPopup message = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, panel.getLabel()).createPopup();
                if (vcsContext.getEditor() != null) {
                    vcsContext.getEditor().showPopupInBestPositionFor(message);
                }
                else {
                    message.showCenteredInCurrentWindow(vcsContext.getProject());
                }
            }
        }
    }

    private static String createMessage(VcsRevisionDescription description, VirtualFile vf) {
        return "<html><head>" + UIUtil.getCssFontDeclaration(UIUtil.getLabelFont()) + "</head><body>" +
            VcsLocalize.currentVersionText(
                description.getAuthor(),
                DateFormatUtil.formatPrettyDateTime(description.getRevisionDate()),
                description.getCommitMessage(),
                description.getRevisionNumber().asString(),
                vf.getName()
            ) + "</body></html>";
    }

    @Override
    protected void update(@Nonnull VcsContext vcsContext, @Nonnull Presentation presentation) {
        AbstractVcs vcs = AbstractShowDiffAction.isEnabled(vcsContext, null);
        presentation.setEnabled(vcs != null);
    }

    static class NotificationPanel extends JPanel {
        protected final JEditorPane myLabel;

        public NotificationPanel() {
            super(new BorderLayout());

            myLabel = new JEditorPane(UIUtil.HTML_MIME, "");
            myLabel.setEditable(false);
            myLabel.setFont(UIUtil.getToolTipFont());

            setBorder(BorderFactory.createEmptyBorder(1, 15, 1, 15));

            add(myLabel, BorderLayout.CENTER);
            myLabel.setBackground(getBackground());
        }

        public void setText(String text) {
            myLabel.setText(text);
        }

        public JEditorPane getLabel() {
            return myLabel;
        }

        @Override
        public Color getBackground() {
            Color color = TargetAWT.to(EditorColorsManager.getInstance()
                .getGlobalScheme()
                .getColor(EditorColors.NOTIFICATION_INFORMATION_BACKGROUND));
            return color == null ? new Color(0xffffcc) : color;
        }
    }
}
