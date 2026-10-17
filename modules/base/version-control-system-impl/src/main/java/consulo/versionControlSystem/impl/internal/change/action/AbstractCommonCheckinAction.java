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
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.AbstractVcsAction;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.action.DescindingFilesFilter;
import consulo.versionControlSystem.impl.internal.change.ui.UnifiedChangesViewManager;
import consulo.versionControlSystem.impl.internal.commit.UnifiedCommitChangeListDialog;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        Project project = Objects.requireNonNull(context.getProject());

        if (ChangeListManager.getInstance(project).isFreezedWithNotification("Can not " + getMnemonicsFreeActionName(context) + " now")) {
            LOG.debug("ChangeListManager is freezed. returning.");
        }
        else if (ProjectLevelVcsManager.getInstance(project).isBackgroundVcsOperationRunning()) {
            LOG.debug("Background operation is running. returning.");
        }
        else {
            FilePath[] roots = prepareRootsForCommit(getRoots(context), project);

            // wait for the asynchronous save to finish before showing the commit dialog
            UIAccess uiAccess = UIAccess.current();
            project.getApplication().saveAllWithProgress(uiAccess).whenComplete((result, throwable) -> uiAccess.give(() ->
                ChangeListManager.getInstance(project).invokeAfterUpdate(
                    () -> performCheckIn(context, project, roots),
                    InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
                    VcsLocalize.waitingChangelistsUpdateForShowCommitDialogMessage().get(),
                    Application.get().getCurrentModalityState()
                )));
        }
    }

    @RequiredUIAccess
    protected void performCheckIn(VcsContext context, Project project, FilePath[] roots) {
        LOG.debug("invoking commit dialog after update");
        LocalChangeList initialSelection = getInitiallySelectedChangeList(context, project);
        CommitExecutor executor = getExecutor(project);
        List<Object> included = getIncludedChanges(context, project, roots, initialSelection);

        if (executor == null
            && project.getInstance(ChangesViewManager.class) instanceof UnifiedChangesViewManager changesViewManager
            && changesViewManager.activateCommitPanel(included, isForceUpdateCommitStateFromContext())) {
            return;
        }

        if (included.isEmpty()) {
            MessageBoxes.okInfo(VcsLocalize.commitDialogNoChangesDetectedText())
                .title(VcsLocalize.commitDialogNoChangesDetectedTitle())
                .showAsync();
            return;
        }

        UnifiedCommitChangeListDialog.show(project, included, initialSelection, executor);
    }

    private static List<Object> getIncludedChanges(VcsContext context, Project project, FilePath[] roots, @Nullable LocalChangeList initialSelection) {
        Change[] selectedChanges = context.getSelectedChanges();
        List<VirtualFile> selectedUnversioned = context.getSelectedUnversionedFiles();

        List<Object> result = new ArrayList<>();
        if (!ArrayUtil.isEmpty(selectedChanges) || !selectedUnversioned.isEmpty()) {
            if (selectedChanges != null) {
                result.addAll(Arrays.asList(selectedChanges));
            }
            result.addAll(selectedUnversioned);
            return result;
        }

        ChangeListManager manager = ChangeListManager.getInstance(project);
        LocalChangeList changeList = initialSelection != null ? initialSelection : manager.getDefaultChangeList();
        Set<Change> changesUnderRoots = new LinkedHashSet<>();
        for (FilePath root : roots) {
            changesUnderRoots.addAll(manager.getChangesIn(root));
        }
        for (Change change : changeList.getChanges()) {
            if (changesUnderRoots.contains(change)) {
                result.add(change);
            }
        }
        return result;
    }

    protected boolean isForceUpdateCommitStateFromContext() {
        return false;
    }

    @RequiredUIAccess
    protected FilePath[] prepareRootsForCommit(FilePath[] roots, Project project) {
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
            presentation.setText(getActionName(vcsContext).appendEllipsis());
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
