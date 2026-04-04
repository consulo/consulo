/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.AbstractVcsAction;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.action.DescindingFilesFilter;
import consulo.versionControlSystem.impl.internal.commit.CommitChangeListDialog;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Stream;

public abstract class AbstractCommonCheckinAction extends AbstractVcsAction {
    private static final Logger LOG = Logger.getInstance(AbstractCommonCheckinAction.class);

    protected AbstractCommonCheckinAction(
        LocalizeValue text,
        LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(VcsContext context) {
        LOG.debug("actionPerformed. ");
        Project project = ObjectUtil.notNull(context.getProject());

        if (ChangeListManager.getInstance(project).isFreezedWithNotification("Can not " + getMnemonicsFreeActionName(context) + " now")) {
            LOG.debug("ChangeListManager is freezed. returning.");
        }
        else if (ProjectLevelVcsManager.getInstance(project).isBackgroundVcsOperationRunning()) {
            LOG.debug("Background operation is running. returning.");
        }
        else {
            FilePath[] roots = prepareRootsForCommit(getRoots(context), project);
            ChangeListManager.getInstance(project).invokeAfterUpdate(
                () -> performCheckIn(context, project, roots),
                InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
                VcsLocalize.waitingChangelistsUpdateForShowCommitDialogMessage().get(),
                Application.get().getCurrentModalityState()
            );
        }
    }

    @RequiredUIAccess
    protected void performCheckIn(VcsContext context, Project project, FilePath[] roots) {
        LOG.debug("invoking commit dialog after update");
        LocalChangeList initialSelection = getInitiallySelectedChangeList(context, project);
        Change[] changes = context.getSelectedChanges();

        if (changes != null && changes.length > 0) {
            CommitChangeListDialog.commitChanges(project, Arrays.asList(changes), initialSelection, getExecutor(project), null);
        }
        else {
            CommitChangeListDialog.commitPaths(project, Arrays.asList(roots), initialSelection, getExecutor(project), null);
        }
    }

    @RequiredUIAccess
    protected FilePath[] prepareRootsForCommit(FilePath[] roots, Project project) {
        Application.get().saveAllWithProgress(UIAccess.current());

        return DescindingFilesFilter.filterDescindingFiles(roots, project);
    }

    protected LocalizeValue getMnemonicsFreeActionName(VcsContext context) {
        return getActionName(context);
    }

    protected @Nullable CommitExecutor getExecutor(Project project) {
        return null;
    }

    protected @Nullable LocalChangeList getInitiallySelectedChangeList(VcsContext context, Project project) {
        LocalChangeList result;
        ChangeListManager manager = ChangeListManager.getInstance(project);
        ChangeList[] changeLists = context.getSelectedChangeLists();

        if (!ArrayUtil.isEmpty(changeLists)) {
            // convert copy to real
            result = manager.findChangeList(changeLists[0].getName());
        }
        else {
            Change[] changes = context.getSelectedChanges();
            result = !ArrayUtil.isEmpty(changes) ? manager.getChangeList(changes[0]) : manager.getDefaultChangeList();
        }

        return result;
    }

    protected abstract LocalizeValue getActionName(VcsContext dataContext);

    protected abstract FilePath[] getRoots(VcsContext dataContext);

    protected abstract boolean approximatelyHasRoots(VcsContext dataContext);

    @Override
    protected void update(VcsContext vcsContext, Presentation presentation) {
        Project project = vcsContext.getProject();

        if (project == null || !ProjectLevelVcsManager.getInstance(project).hasActiveVcss()) {
            presentation.setEnabledAndVisible(false);
        }
        else if (!approximatelyHasRoots(vcsContext)) {
            presentation.setEnabled(false);
        }
        else {
            presentation.setTextValue(getActionName(vcsContext).map(string -> string + "..."));
            presentation.setEnabled(!ProjectLevelVcsManager.getInstance(project).isBackgroundVcsOperationRunning());
            presentation.setVisible(true);
        }
    }

    protected static FilePath[] getAllContentRoots(VcsContext context) {
        return Stream.of(ProjectLevelVcsManager.getInstance(context.getProject()).getAllVersionedRoots())
            .map(VcsUtil::getFilePath)
            .toArray(FilePath[]::new);
    }
}