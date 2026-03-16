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
package consulo.versionControlSystem.distributed.branch;

import consulo.application.Application;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.internal.BranchListPopup;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.internal.FlatSpeedSearchPopupFactory;
import org.jspecify.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.List;
import java.util.function.Predicate;

public abstract class DvcsBranchPopup<Repo extends Repository> {
    
    protected final Project myProject;
    
    protected final AbstractRepositoryManager<Repo> myRepositoryManager;
    
    protected final DvcsSyncSettings myVcsSettings;
    
    protected final AbstractVcs myVcs;
    
    protected final DvcsMultiRootBranchConfig<Repo> myMultiRootBranchConfig;

    
    protected final Repo myCurrentRepository;
    
    protected final BranchListPopup myPopup;
    protected final boolean myIsInSpecificRepository;

    protected DvcsBranchPopup(
        Repo currentRepository,
        AbstractRepositoryManager<Repo> repositoryManager,
        DvcsMultiRootBranchConfig<Repo> multiRootBranchConfig,
        DvcsSyncSettings vcsSettings,
        Predicate<AnAction> preselectActionCondition,
        @Nullable String dimensionKey
    ) {
        myProject = currentRepository.getProject();
        myCurrentRepository = currentRepository;
        myRepositoryManager = repositoryManager;
        myVcs = currentRepository.getVcs();
        myVcsSettings = vcsSettings;
        myMultiRootBranchConfig = multiRootBranchConfig;

        myIsInSpecificRepository =
            myRepositoryManager.moreThanOneRoot() && myVcsSettings.getSyncSetting() == DvcsSyncSettings.Value.DONT_SYNC;
        LocalizeValue title = myIsInSpecificRepository
            ? DistributedVcsLocalize.branchPopupVcsNameBranchesInRepo(myVcs.getDisplayName(), DvcsUtil.getShortRepositoryName(currentRepository))
            : DistributedVcsLocalize.branchPopupVcsNameBranches(myVcs.getDisplayName());

        FlatSpeedSearchPopupFactory popupFactory = FlatSpeedSearchPopupFactory.getInstance();
        myPopup = (BranchListPopup) popupFactory.createBranchPopup(
            title.get(),
            myProject,
            preselectActionCondition,
            createActions(),
            dimensionKey
        );

        initBranchSyncPolicyIfNotInitialized();
        warnThatBranchesDivergedIfNeeded();
    }

    
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
        NotificationService.getInstance()
            .newInfo(VcsNotifier.IMPORTANT_ERROR_NOTIFICATION)
            .title(DistributedVcsLocalize.notificationSynchedBranchesTitle())
            .content(DistributedVcsLocalize.notificationSynchedBranchesContent(myVcs.getDisplayName()))
            .hyperlinkListener(new NotificationListener() {
                @Override
                @RequiredUIAccess
                public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
                    if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        Application.get().getInstance(ShowConfigurableService.class).showAndSelect(myProject, "vcs." + myVcs.getId());

                        if (myVcsSettings.getSyncSetting() == DvcsSyncSettings.Value.DONT_SYNC) {
                            notification.expire();
                        }
                    }
                }
            })
            .notify(myProject);
    }

    
    private ActionGroup createActions() {
        ActionGroup.Builder popupGroupBuilder = ActionGroup.newImmutableBuilder();
        AbstractRepositoryManager<Repo> repositoryManager = myRepositoryManager;
        if (repositoryManager.moreThanOneRoot()) {
            if (userWantsSyncControl()) {
                fillWithCommonRepositoryActions(popupGroupBuilder, repositoryManager);
            }
            else {
                fillPopupWithCurrentRepositoryActions(popupGroupBuilder, createRepositoriesActions());
            }
        }
        else {
            fillPopupWithCurrentRepositoryActions(popupGroupBuilder, null);
        }
        popupGroupBuilder.addSeparator();
        return popupGroupBuilder.build();
    }

    private boolean userWantsSyncControl() {
        return (myVcsSettings.getSyncSetting() != DvcsSyncSettings.Value.DONT_SYNC);
    }

    protected abstract void fillWithCommonRepositoryActions(
        ActionGroup.Builder popupGroup,
        AbstractRepositoryManager<Repo> repositoryManager
    );

    
    protected List<Repo> filterRepositoriesNotOnThisBranch(String branch, List<Repo> allRepositories) {
        return ContainerUtil.filter(allRepositories, repository -> !branch.equals(repository.getCurrentBranchName()));
    }

    private void warnThatBranchesDivergedIfNeeded() {
        if (myRepositoryManager.moreThanOneRoot() && myMultiRootBranchConfig.diverged() && userWantsSyncControl()) {
            myPopup.setWarning(DistributedVcsLocalize.branchPopupWarningBranchesHaveDiverged());
        }
    }

    
    protected abstract ActionGroup createRepositoriesActions();

    protected boolean highlightCurrentRepo() {
        return !userWantsSyncControl() || myMultiRootBranchConfig.diverged();
    }

    protected abstract void fillPopupWithCurrentRepositoryActions(
        ActionGroup.Builder popupGroup,
        @Nullable ActionGroup actions
    );

    public static class MyMoreIndex {
        public static final int MAX_REPO_NUM = 8;
        public static final int DEFAULT_REPO_NUM = 5;
        public static final int MAX_BRANCH_NUM = 8;
    }
}
