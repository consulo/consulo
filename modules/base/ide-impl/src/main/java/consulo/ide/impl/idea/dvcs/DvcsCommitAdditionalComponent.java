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
package consulo.ide.impl.idea.dvcs;

import consulo.application.progress.ProgressManager;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.NonFocusableCheckBox;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.distributed.DvcsBundle;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
  private  Map<VirtualFile, String> myMessagesForRoots;

  public DvcsCommitAdditionalComponent(@Nonnull final Project project, @Nonnull CheckinProjectPanel panel) {
    myCheckinPanel = panel;
    myPanel = new JPanel(new GridBagLayout());
    final Insets insets = new Insets(2, 2, 2, 2);
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

    myAmend = new NonFocusableCheckBox(DvcsBundle.message("commit.amend"));
    myAmend.setMnemonic('m');
    myAmend.setToolTipText(DvcsBundle.message("commit.amend.tooltip"));
    myPreviousMessage = myCheckinPanel.getCommitMessage();

    myAmend.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
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
      }
    });
    myPanel.add(myAmend, c);
  }

  private String constructAmendedMessage() {
    Set<VirtualFile> selectedRoots = getVcsRoots(getSelectedFilePaths());        // get only selected files
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

  public JComponent getComponent() {
    return myPanel;
  }

  public void refresh() {
    myAmend.setSelected(false);
  }

  @NonNls
  private void loadMessagesInModalTask(@Nonnull Project project) {
    try {
      myMessagesForRoots = ProgressManager.getInstance().runProcessWithProgressSynchronously(
        () -> getLastCommitMessages(),
        "Reading commit message...",
        false,
        project
      );
    }
    catch (VcsException e) {
      Messages.showErrorDialog(
        getComponent(),
        "Couldn't load commit message of the commit to amend.\n" + e.getMessage(),
        "Commit ReflectionMessage not Loaded"
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

  @Nullable
  private Map<VirtualFile, String> getLastCommitMessages() throws VcsException {
    Map<VirtualFile, String> messagesForRoots = new HashMap<>();
    Collection<VirtualFile> roots = myCheckinPanel.getRoots(); //all committed vcs roots, not only selected
    final Ref<VcsException> exception = Ref.create();
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
    return ContainerUtil.map(myCheckinPanel.getFiles(), (Function<File, FilePath>)file -> new FilePathImpl(file, file.isDirectory()));
  }

  @Nonnull
  protected abstract Set<VirtualFile> getVcsRoots(@Nonnull Collection<FilePath> files);

  @Nullable
  protected abstract String getLastCommitMessage(@Nonnull VirtualFile repo) throws VcsException;

  public boolean isAmend() {
    return myAmend.isSelected();
  }
}
