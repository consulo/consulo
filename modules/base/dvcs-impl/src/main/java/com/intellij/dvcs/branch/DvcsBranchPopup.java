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
package com.intellij.dvcs.branch;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.AbstractRepositoryManager;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.ui.BranchActionGroupPopup;
import com.intellij.dvcs.ui.LightActionGroup;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.HyperlinkEvent;
import java.util.List;

public abstract class DvcsBranchPopup<Repo extends Repository> {
  @Nonnull
  protected final Project myProject;
  @Nonnull
  protected final AbstractRepositoryManager<Repo> myRepositoryManager;
  @Nonnull
  protected final DvcsSyncSettings myVcsSettings;
  @Nonnull
  protected final AbstractVcs myVcs;
  @Nonnull
  protected final DvcsMultiRootBranchConfig<Repo> myMultiRootBranchConfig;

  @Nonnull
  protected final Repo myCurrentRepository;
  @Nonnull
  protected final ListPopupImpl myPopup;
  @Nonnull
  protected final String myRepoTitleInfo;

  protected DvcsBranchPopup(@Nonnull Repo currentRepository,
                            @Nonnull AbstractRepositoryManager<Repo> repositoryManager,
                            @Nonnull DvcsMultiRootBranchConfig<Repo> multiRootBranchConfig,
                            @Nonnull DvcsSyncSettings vcsSettings,
                            @Nonnull Condition<AnAction> preselectActionCondition,
                            @Nullable String dimensionKey) {
    myProject = currentRepository.getProject();
    myCurrentRepository = currentRepository;
    myRepositoryManager = repositoryManager;
    myVcs = currentRepository.getVcs();
    myVcsSettings = vcsSettings;
    myMultiRootBranchConfig = multiRootBranchConfig;
    String title = myVcs.getDisplayName() + " Branches";
    myRepoTitleInfo = (myRepositoryManager.moreThanOneRoot() && myVcsSettings.getSyncSetting() == DvcsSyncSettings.Value.DONT_SYNC) ? " in " + DvcsUtil.getShortRepositoryName(currentRepository) : "";
    myPopup = new BranchActionGroupPopup(title + myRepoTitleInfo, myProject, preselectActionCondition, createActions(), dimensionKey);

    initBranchSyncPolicyIfNotInitialized();
    warnThatBranchesDivergedIfNeeded();
  }

  @Nonnull
  public ListPopup asListPopup() {
    return myPopup;
  }

  private void initBranchSyncPolicyIfNotInitialized() {
    if (myRepositoryManager.moreThanOneRoot() && myVcsSettings.getSyncSetting() == DvcsSyncSettings.Value.NOT_DECIDED) {
      if (!myMultiRootBranchConfig.diverged()) {
        notifyAboutSyncedBranches();
        myVcsSettings.setSyncSetting(DvcsSyncSettings.Value.SYNC);
      }
      else {
        myVcsSettings.setSyncSetting(DvcsSyncSettings.Value.DONT_SYNC);
      }
    }
  }

  private void notifyAboutSyncedBranches() {
    String description = "You have several " +
                         myVcs.getDisplayName() +
                         " roots in the project and they all are checked out at the same branch. " +
                         "We've enabled synchronous branch control for the project. <br/>" +
                         "If you wish to control branches in different roots separately, " +
                         "you may <a href='settings'>disable</a> the setting.";
    NotificationListener listener = new NotificationListener() {
      @Override
      public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          ShowSettingsUtil.getInstance().showSettingsDialog(myProject, myVcs.getDisplayName());
          if (myVcsSettings.getSyncSetting() == DvcsSyncSettings.Value.DONT_SYNC) {
            notification.expire();
          }
        }
      }
    };
    VcsNotifier.getInstance(myProject).notifyImportantInfo("Synchronous branch control enabled", description, listener);
  }

  @Nonnull
  private ActionGroup createActions() {
    LightActionGroup popupGroup = new LightActionGroup();
    AbstractRepositoryManager<Repo> repositoryManager = myRepositoryManager;
    if (repositoryManager.moreThanOneRoot()) {
      if (userWantsSyncControl()) {
        fillWithCommonRepositoryActions(popupGroup, repositoryManager);
      }
      else {
        fillPopupWithCurrentRepositoryActions(popupGroup, createRepositoriesActions());
      }
    }
    else {
      fillPopupWithCurrentRepositoryActions(popupGroup, null);
    }
    popupGroup.addSeparator();
    return popupGroup;
  }

  private boolean userWantsSyncControl() {
    return (myVcsSettings.getSyncSetting() != DvcsSyncSettings.Value.DONT_SYNC);
  }

  protected abstract void fillWithCommonRepositoryActions(@Nonnull LightActionGroup popupGroup, @Nonnull AbstractRepositoryManager<Repo> repositoryManager);

  @Nonnull
  protected List<Repo> filterRepositoriesNotOnThisBranch(@Nonnull final String branch, @Nonnull List<Repo> allRepositories) {
    return ContainerUtil.filter(allRepositories, repository -> !branch.equals(repository.getCurrentBranchName()));
  }

  private void warnThatBranchesDivergedIfNeeded() {
    if (myRepositoryManager.moreThanOneRoot() && myMultiRootBranchConfig.diverged() && userWantsSyncControl()) {
      myPopup.setWarning("Branches have diverged");
    }
  }

  @Nonnull
  protected abstract LightActionGroup createRepositoriesActions();

  protected boolean highlightCurrentRepo() {
    return !userWantsSyncControl() || myMultiRootBranchConfig.diverged();
  }

  protected abstract void fillPopupWithCurrentRepositoryActions(@Nonnull LightActionGroup popupGroup, @Nullable LightActionGroup actions);

  public static class MyMoreIndex {
    public static final int MAX_REPO_NUM = 8;
    public static final int DEFAULT_REPO_NUM = 5;
    public static final int MAX_BRANCH_NUM = 8;
  }
}
