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
package consulo.ide.impl.idea.dvcs;

import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.RepositoryManager;
import consulo.ide.impl.idea.dvcs.ui.DvcsBundle;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.vcs.checkin.CheckinProjectPanel;
import consulo.vcs.FilePath;
import consulo.vcs.VcsConfiguration;
import consulo.vcs.VcsException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.NonFocusableCheckBox;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.vcs.util.VcsUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides a checkbox to amend current commit to the previous commit.
 * Selecting a checkbox loads the previous commit message from the provider, and substitutes current message in the editor,
 * unless it was already modified by user.
 */
public abstract class AmendComponent {

  private static final Logger LOG = Logger.getInstance(AmendComponent.class);

  @Nonnull
  private final RepositoryManager<? extends Repository> myRepoManager;
  @Nonnull
  private final CheckinProjectPanel myCheckinPanel;
  @Nonnull
  private final JCheckBox myAmend;
  @Nonnull
  private final String myPreviousMessage;

  @javax.annotation.Nullable
  private Map<VirtualFile, String> myMessagesForRoots;
  @javax.annotation.Nullable
  private String myAmendedMessage;

  public AmendComponent(@Nonnull Project project,
                        @Nonnull RepositoryManager<? extends Repository> repoManager,
                        @Nonnull CheckinProjectPanel panel) {
    this(project, repoManager, panel, DvcsBundle.message("commit.amend"));
  }

  public AmendComponent(@Nonnull Project project,
                        @Nonnull RepositoryManager<? extends Repository> repoManager,
                        @Nonnull CheckinProjectPanel panel,
                        @Nonnull String title) {
    myRepoManager = repoManager;
    myCheckinPanel = panel;
    myAmend = new NonFocusableCheckBox(title);
    myAmend.setMnemonic('m');
    myAmend.setToolTipText(DvcsBundle.message("commit.amend.tooltip"));
    myPreviousMessage = myCheckinPanel.getCommitMessage();

    myAmend.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myAmend.isSelected()) {
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
        else {
          // there was the amended message, but user has changed it => not reverting
          if (myCheckinPanel.getCommitMessage().equals(myAmendedMessage)) {
            myCheckinPanel.setCommitMessage(myPreviousMessage);
          }
        }
      }
    });
  }

  @javax.annotation.Nullable
  private String constructAmendedMessage() {
    Set<VirtualFile> selectedRoots = getVcsRoots(getSelectedFilePaths()); // get only selected files
    LinkedHashSet<String> messages = ContainerUtil.newLinkedHashSet();
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

  public void refresh() {
    myAmend.setSelected(false);
  }

  @Nonnull
  public Component getComponent() {
    return myAmend;
  }

  @Nonnull
  public JCheckBox getCheckBox() {
    return myAmend;
  }

  private void loadMessagesInModalTask(@Nonnull Project project) {
    try {
      myMessagesForRoots = ProgressManager.getInstance().runProcessWithProgressSynchronously(this::getLastCommitMessages,
                                                                                             "Reading Commit Message...", true, project);
    }
    catch (VcsException e) {
      Messages.showErrorDialog(project, "Couldn't load commit message of the commit to amend.\n" + e.getMessage(),
                               "Commit Message not Loaded");
      LOG.info(e);
    }
  }

  private void substituteCommitMessage(@Nonnull String newMessage) {
    if (!StringUtil.equalsIgnoreWhitespaces(myPreviousMessage, newMessage)) {
      VcsConfiguration.getInstance(myCheckinPanel.getProject()).saveCommitMessage(myPreviousMessage);
      myCheckinPanel.setCommitMessage(newMessage);
    }
  }

  @javax.annotation.Nullable
  private Map<VirtualFile, String> getLastCommitMessages() throws VcsException {
    Map<VirtualFile, String> messagesForRoots = new HashMap<>();
    // load all vcs roots visible in the commit dialog (not only selected ones), to avoid another loading task if selection changes
    for (VirtualFile root : getAffectedRoots()) {
      String message = getLastCommitMessage(root);
      messagesForRoots.put(root, message);
    }
    return messagesForRoots;
  }

  @Nonnull
  protected Collection<VirtualFile> getAffectedRoots() {
    return myRepoManager.getRepositories().stream().
            filter(repo -> !repo.isFresh()).
            map(Repository::getRoot).
            filter(root -> myCheckinPanel.getRoots().contains(root)).
            collect(Collectors.toList());
  }

  @Nonnull
  private List<FilePath> getSelectedFilePaths() {
    return ContainerUtil.map(myCheckinPanel.getFiles(), VcsUtil::getFilePath);
  }

  @Nonnull
  protected abstract Set<VirtualFile> getVcsRoots(@Nonnull Collection<FilePath> files);

  @javax.annotation.Nullable
  protected abstract String getLastCommitMessage(@Nonnull VirtualFile repo) throws VcsException;

  public boolean isAmend() {
    return myAmend.isSelected();
  }
}
