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
package consulo.versionControlSystem.distributed.ui;

import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.NonFocusableCheckBox;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Function;

public abstract class DvcsCommitAdditionalComponent implements RefreshableOnComponent {
    private static final Logger log = Logger.getInstance(DvcsCommitAdditionalComponent.class);

    protected final JPanel myPanel;
    protected final JCheckBox myAmend;
    @Nullable
    private String myPreviousMessage;
    @Nullable
    private String myAmendedMessage;
    @Nonnull
    protected final CheckinProjectPanel myCheckinPanel;
    @Nullable
    private Map<VirtualFile, String> myMessagesForRoots;

    public DvcsCommitAdditionalComponent(@Nonnull Project project, @Nonnull CheckinProjectPanel panel) {
        myCheckinPanel = panel;
        myPanel = new JPanel(new GridBagLayout());
        Insets insets = new Insets(2, 2, 2, 2);
        // add amend checkbox
        GridBagConstraints c = new GridBagConstraints();
        //todo change to MigLayout
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = insets;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        myAmend = new NonFocusableCheckBox(DistributedVcsLocalize.commitAmend().get());
        myAmend.setMnemonic('m');
        myAmend.setToolTipText(DistributedVcsLocalize.commitAmendTooltip().get());
        myPreviousMessage = myCheckinPanel.getCommitMessage();

        myAmend.addActionListener(e -> {
            if (myAmend.isSelected()) {
                if (myPreviousMessage.equals(myCheckinPanel.getCommitMessage())) { // if user has already typed something, don't revert it
                    if (myMessagesForRoots == null) {
                        loadMessagesInModalTask(project);      //load all commit messages for all repositories
                    }
                    String message = constructAmendedMessage();
                    if (!StringUtil.isEmptyOrSpaces(message)) {
                        myAmendedMessage = message;
                        substituteCommitMessage(myAmendedMessage);
                    }
                }
            }
            else {
                // there was the amended message, but user has changed it => not reverting
                if (myCheckinPanel.getCommitMessage().equals(myAmendedMessage)) {
                    myCheckinPanel.setCommitMessage(myPreviousMessage);
                }
            }
        });
        myPanel.add(myAmend, c);
    }

    private String constructAmendedMessage() {
        Set<VirtualFile> selectedRoots = getVcsRoots(getSelectedFilePaths());        // get only selected files
        LinkedHashSet<String> messages = new LinkedHashSet<>();
        if (myMessagesForRoots != null) {
            for (VirtualFile root : selectedRoots) {
                String message = myMessagesForRoots.get(root);
                if (message != null) {
                    messages.add(message);
                }
            }
        }
        return DvcsUtil.joinMessagesOrNull(messages);
    }

    @Override
    public JComponent getComponent() {
        return myPanel;
    }

    @Override
    public void refresh() {
        myAmend.setSelected(false);
    }

    @RequiredUIAccess
    private void loadMessagesInModalTask(@Nonnull Project project) {
        try {
            myMessagesForRoots = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                this::getLastCommitMessages,
                VcsLocalize.amendCommitLoadMessageTaskTitle(),
                false,
                project
            );
        }
        catch (VcsException e) {
            Messages.showErrorDialog(
                getComponent(),
                VcsLocalize.amendCommitLoadMessageErrorText(e.getMessage()).get(),
                VcsLocalize.amendCommitLoadMessageErrorTitle().get()
            );
            log.info(e);
        }
    }

    private void substituteCommitMessage(@Nonnull String newMessage) {
        myPreviousMessage = myCheckinPanel.getCommitMessage();
        if (!myPreviousMessage.trim().equals(newMessage.trim())) {
            myCheckinPanel.setCommitMessage(newMessage);
        }
    }

    @Nonnull
    private Map<VirtualFile, String> getLastCommitMessages() throws VcsException {
        Map<VirtualFile, String> messagesForRoots = new HashMap<>();
        Collection<VirtualFile> roots = myCheckinPanel.getRoots(); //all committed vcs roots, not only selected
        SimpleReference<VcsException> exception = SimpleReference.create();
        for (VirtualFile root : roots) {
            String message = getLastCommitMessage(root);
            messagesForRoots.put(root, message);
        }
        if (!exception.isNull()) {
            throw exception.get();
        }
        return messagesForRoots;
    }

    @Nonnull
    private List<FilePath> getSelectedFilePaths() {
        return ContainerUtil.map(myCheckinPanel.getFiles(), (Function<File, FilePath>) file -> new FilePathImpl(file, file.isDirectory()));
    }

    @Nonnull
    protected abstract Set<VirtualFile> getVcsRoots(@Nonnull Collection<FilePath> files);

    @Nullable
    protected abstract String getLastCommitMessage(@Nonnull VirtualFile repo) throws VcsException;

    public boolean isAmend() {
        return myAmend.isSelected();
    }
}
