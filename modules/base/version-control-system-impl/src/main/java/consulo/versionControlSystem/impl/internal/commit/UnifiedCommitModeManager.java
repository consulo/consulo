/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsMappingListener;
import consulo.versionControlSystem.VcsType;
import consulo.versionControlSystem.commit.CommitMode;
import consulo.versionControlSystem.commit.CommitModeListener;
import consulo.versionControlSystem.commit.CommitModeManager;
import consulo.versionControlSystem.commit.CommitModeSettingsListener;
import consulo.versionControlSystem.commit.CommitModeType;
import consulo.versionControlSystem.commit.ModalCommitMode;
import consulo.versionControlSystem.commit.NonModalCommitMode;
import consulo.versionControlSystem.commit.PendingCommitMode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@Singleton
@ServiceImpl
public class UnifiedCommitModeManager implements CommitModeManager, Disposable {
    private static final String FORCE_MODAL_COMMIT = "vcs.force.modal.commit";

    private final Project myProject;
    private final Application myApplication;
    private final VcsApplicationSettings mySettings;

    private volatile CommitMode myCommitMode = PendingCommitMode.INSTANCE;

    private boolean mySubscribed;

    @Inject
    public UnifiedCommitModeManager(Project project, Application application, VcsApplicationSettings settings) {
        myProject = project;
        myApplication = application;
        mySettings = settings;
    }

    @Override
    public CommitMode getCurrentCommitMode() {
        return myCommitMode;
    }

    @RequiredUIAccess
    void initialize() {
        if (!mySubscribed) {
            mySubscribed = true;

            MessageBusConnection applicationConnection = myApplication.getMessageBus().connect(this);
            applicationConnection.subscribe(CommitModeSettingsListener.class, this::updateCommitMode);

            MessageBusConnection projectConnection = myProject.getMessageBus().connect(this);
            projectConnection.subscribe(VcsMappingListener.class, this::scheduleUpdateCommitMode);
        }

        updateCommitMode();
    }

    private void scheduleUpdateCommitMode() {
        myApplication.invokeLater(this::updateCommitMode, ModalityState.nonModal(), myProject.getDisposed());
    }

    @RequiredUIAccess
    private void updateCommitMode() {
        if (myProject.isDisposed()) {
            return;
        }

        CommitMode newCommitMode = computeCommitMode();
        if (newCommitMode.equals(myCommitMode)) {
            return;
        }

        myCommitMode = newCommitMode;

        myProject.getMessageBus().syncPublisher(CommitModeListener.class).commitModeChanged();
    }

    private CommitMode computeCommitMode() {
        AbstractVcs[] activeVcses = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
        if (activeVcses.length == 0) {
            return PendingCommitMode.INSTANCE;
        }

        if (Boolean.getBoolean(FORCE_MODAL_COMMIT) || !myProject.isOpen()) {
            return ModalCommitMode.INSTANCE;
        }

        CommitMode commitMode = fromSettings(activeVcses);

        if (activeVcses.length == 1) {
            CommitMode forced = activeVcses[0].getForcedCommitMode(commitMode);
            if (forced != null) {
                return forced;
            }
        }
        return commitMode;
    }

    private CommitMode fromSettings(AbstractVcs[] activeVcses) {
        CommitModeType type = mySettings.COMMIT_MODE;

        if (type == CommitModeType.DIALOG || !canSetNonModal(activeVcses)) {
            return ModalCommitMode.INSTANCE;
        }

        return new NonModalCommitMode(type == CommitModeType.TOOL_WINDOW, mySettings.COMMIT_TOGGLE_MODE);
    }

    private static boolean canSetNonModal(AbstractVcs[] activeVcses) {
        for (AbstractVcs vcs : activeVcses) {
            if (vcs.getType() != VcsType.distributed) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void dispose() {
    }
}
