/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.dvcs.cherrypick;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.collection.Sets;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.distributed.VcsCherryPicker;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsCherryPickManager {
    private static final Logger LOG = Logger.getInstance(VcsCherryPickManager.class);
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final ProjectLevelVcsManager myProjectLevelVcsManager;
    @Nonnull
    private final NotificationService myNotificationService;
    @Nonnull
    private final Set<CommitId> myIdsInProgress = Sets.newConcurrentHashSet();

    @Inject
    public VcsCherryPickManager(
        @Nonnull Project project,
        @Nonnull ProjectLevelVcsManager projectLevelVcsManager,
        @Nonnull NotificationService notificationService
    ) {
        myProject = project;
        myProjectLevelVcsManager = projectLevelVcsManager;
        myNotificationService = notificationService;
    }

    public void cherryPick(@Nonnull VcsLog log) {
        log.requestSelectedDetails(details -> ProgressManager.getInstance()
            .run(new CherryPickingTask(myProject, ContainerUtil.reverse(details))), null);
    }

    public boolean isCherryPickAlreadyStartedFor(@Nonnull List<CommitId> commits) {
        for (CommitId commit : commits) {
            if (myIdsInProgress.contains(commit)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private VcsCherryPicker getCherryPickerForCommit(@Nonnull VcsFullCommitDetails commitDetails) {
        AbstractVcs vcs = myProjectLevelVcsManager.getVcsFor(commitDetails.getRoot());
        if (vcs == null) {
            return null;
        }
        VcsKey key = vcs.getKeyInstanceMethod();
        return getCherryPickerFor(key);
    }

    @Nullable
    public VcsCherryPicker getCherryPickerFor(@Nonnull VcsKey key) {
        return ContainerUtil.find(myProject.getExtensionList(VcsCherryPicker.class), picker -> picker.getSupportedVcs().equals(key));
    }

    private class CherryPickingTask extends Task.Backgroundable {
        @Nonnull
        private final List<VcsFullCommitDetails> myAllDetailsInReverseOrder;
        @Nonnull
        private final ChangeListManagerEx myChangeListManager;

        @RequiredUIAccess
        public CherryPickingTask(@Nonnull Project project, @Nonnull List<VcsFullCommitDetails> detailsInReverseOrder) {
            super(project, LocalizeValue.localizeTODO("Cherry-Picking"));
            myAllDetailsInReverseOrder = detailsInReverseOrder;
            myChangeListManager = (ChangeListManagerEx) ChangeListManager.getInstance((Project) myProject);
            myChangeListManager.blockModalNotifications();
        }

        @Nullable
        private VcsCherryPicker getCherryPickerOrReportError(@Nonnull VcsFullCommitDetails details) {
            CommitId commitId = new CommitId(details.getId(), details.getRoot());
            if (myIdsInProgress.contains(commitId)) {
                showError("Cherry pick process is already started for commit " +
                    commitId.getHash().toShortString() +
                    " from root " +
                    commitId.getRoot().getName());
                return null;
            }
            myIdsInProgress.add(commitId);

            VcsCherryPicker cherryPicker = getCherryPickerForCommit(details);
            if (cherryPicker == null) {
                showError(
                    "Cherry pick is not supported for commit " + details.getId().toShortString() + " from root " + details.getRoot()
                        .getName());
                return null;
            }
            return cherryPicker;
        }

        public void showError(@Nonnull String message) {
            myNotificationService.newError(VcsNotifier.NOTIFICATION_GROUP_ID)
                .content(LocalizeValue.localizeTODO(message))
                .notify((Project) myProject);
            LOG.warn(message);
        }

        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
            try {
                boolean isOk = true;
                MultiMap<VcsCherryPicker, VcsFullCommitDetails> groupedCommits = createArrayMultiMap();
                for (VcsFullCommitDetails details : myAllDetailsInReverseOrder) {
                    VcsCherryPicker cherryPicker = getCherryPickerOrReportError(details);
                    if (cherryPicker == null) {
                        isOk = false;
                        break;
                    }
                    groupedCommits.putValue(cherryPicker, details);
                }

                if (isOk) {
                    for (Map.Entry<VcsCherryPicker, Collection<VcsFullCommitDetails>> entry : groupedCommits.entrySet()) {
                        entry.getKey().cherryPick(new ArrayList<>(entry.getValue()));
                    }
                }
            }
            finally {
                Application.get().invokeLater(() -> {
                    myChangeListManager.unblockModalNotifications();
                    for (VcsFullCommitDetails details : myAllDetailsInReverseOrder) {
                        myIdsInProgress.remove(new CommitId(details.getId(), details.getRoot()));
                    }
                });
            }
        }

        @Nonnull
        public MultiMap<VcsCherryPicker, VcsFullCommitDetails> createArrayMultiMap() {
            return new MultiMap<>() {
                @Nonnull
                @Override
                protected Collection<VcsFullCommitDetails> createCollection() {
                    return new ArrayList<>();
                }
            };
        }
    }

    public static VcsCherryPickManager getInstance(@Nonnull Project project) {
        return project.getInstance(VcsCherryPickManager.class);
    }
}
