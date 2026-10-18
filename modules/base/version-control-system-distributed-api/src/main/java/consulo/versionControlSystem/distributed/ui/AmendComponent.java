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
package consulo.versionControlSystem.distributed.ui;

import consulo.application.progress.ProgressManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.RepositoryManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides a checkbox to amend current commit to the previous commit.
 * Selecting a checkbox loads the previous commit message from the provider, and substitutes current message in the editor,
 * unless it was already modified by user.
 */
public abstract class AmendComponent {
    private static final Logger LOG = Logger.getInstance(AmendComponent.class);

    private final RepositoryManager<? extends Repository> myRepoManager;
    
    private final CheckinProjectPanel myCheckinPanel;
    
    private final CheckBox myAmend;
    
    private final String myPreviousMessage;

    private @Nullable Map<VirtualFile, String> myMessagesForRoots;
    private @Nullable String myAmendedMessage;
    private boolean myResetting;

    @RequiredUIAccess
    public AmendComponent(
        Project project,
        RepositoryManager<? extends Repository> repoManager,
        CheckinProjectPanel panel
    ) {
        this(project, repoManager, panel, DistributedVcsLocalize.commitAmend());
    }

    @RequiredUIAccess
    public AmendComponent(
        Project project,
        RepositoryManager<? extends Repository> repoManager,
        CheckinProjectPanel panel,
        LocalizeValue title
    ) {
        myRepoManager = repoManager;
        myCheckinPanel = panel;
        myAmend = CheckBox.create(title);
        myAmend.setToolTipText(DistributedVcsLocalize.commitAmendTooltip());
        myPreviousMessage = myCheckinPanel.getCommitMessage();

        myAmend.addValueListener(event -> {
            if (myResetting) {
                return;
            }
            if (Boolean.TRUE.equals(event.getValue())) {
                if (myPreviousMessage.equals(myCheckinPanel.getCommitMessage())) { // if user has already typed something, don't revert it
                    if (myMessagesForRoots == null) {
                        loadMessagesInModalTask(project); // load all commit messages for all repositories
                    }
                    String message = constructAmendedMessage();
                    if (!StringUtil.isEmptyOrSpaces(message)) {
                        myAmendedMessage = message;
                        substituteCommitMessage(myAmendedMessage);
                    }
                }
            }
            else if (myCheckinPanel.getCommitMessage().equals(myAmendedMessage)) {
                // there was the amended message, but user has changed it => not reverting
                myCheckinPanel.setCommitMessage(myPreviousMessage);
            }
        });
    }

    private @Nullable String constructAmendedMessage() {
        Set<VirtualFile> selectedRoots = getVcsRoots(getSelectedFilePaths()); // get only selected files
        SequencedSet<String> messages = new LinkedHashSet<>();
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

    @RequiredUIAccess
    public void refresh() {
        myResetting = true;
        try {
            myAmend.setValue(false);
        }
        finally {
            myResetting = false;
        }
    }

    public Component getComponent() {
        return myAmend;
    }

    public CheckBox getCheckBox() {
        return myAmend;
    }

    @RequiredUIAccess
    private void loadMessagesInModalTask(Project project) {
        try {
            myMessagesForRoots = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                this::getLastCommitMessages,
                VcsLocalize.amendCommitLoadMessageTaskTitle(),
                true,
                project
            );
        }
        catch (VcsException e) {
            MessageBoxes.okError(VcsLocalize.amendCommitLoadMessageErrorText(e.getMessage()))
                .title(VcsLocalize.amendCommitLoadMessageErrorTitle())
                .showAsync();
            LOG.info(e);
        }
    }

    private void substituteCommitMessage(String newMessage) {
        if (!StringUtil.equalsIgnoreWhitespaces(myPreviousMessage, newMessage)) {
            VcsConfiguration.getInstance(myCheckinPanel.getProject()).saveCommitMessage(myPreviousMessage);
            myCheckinPanel.setCommitMessage(newMessage);
        }
    }

    
    private Map<VirtualFile, String> getLastCommitMessages() throws VcsException {
        Map<VirtualFile, String> messagesForRoots = new HashMap<>();
        // load all vcs roots visible in the commit dialog (not only selected ones), to avoid another loading task if selection changes
        for (VirtualFile root : getAffectedRoots()) {
            String message = getLastCommitMessage(root);
            messagesForRoots.put(root, message);
        }
        return messagesForRoots;
    }

    
    protected Collection<VirtualFile> getAffectedRoots() {
        return myRepoManager.getRepositories().stream().
            filter(repo -> !repo.isFresh()).
            map(Repository::getRoot).
            filter(root -> myCheckinPanel.getRoots().contains(root)).
            collect(Collectors.toList());
    }

    
    private List<FilePath> getSelectedFilePaths() {
        return ContainerUtil.map(myCheckinPanel.getFiles(), VcsUtil::getFilePath);
    }

    
    protected abstract Set<VirtualFile> getVcsRoots(Collection<FilePath> files);

    protected abstract @Nullable String getLastCommitMessage(VirtualFile repo) throws VcsException;

    public boolean isAmend() {
        return myAmend.getValueOrError();
    }
}
