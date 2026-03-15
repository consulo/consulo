/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.Streams;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.impl.internal.change.action.AbstractCommonCheckinAction;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ActionImpl(id = "CheckinFiles")
public class CommonCheckinFilesAction extends AbstractCommonCheckinAction {
    public CommonCheckinFilesAction() {
        super(VcsLocalize.actionCheckInFilesText(), LocalizeValue.empty(), null);
    }

    
    @Override
    protected LocalizeValue getActionName(VcsContext dataContext) {
        LocalizeValue actionName = Optional.ofNullable(dataContext.getProject())
            .map(project -> getCommonVcs(getRootsStream(dataContext), project))
            .map(AbstractVcs::getCheckinEnvironment)
            .map(CheckinEnvironment::getCheckinOperationName)
            .orElse(VcsLocalize.vcsCommandNameCheckin());

        return modifyCheckinActionName(dataContext, actionName);
    }

    private LocalizeValue modifyCheckinActionName(VcsContext dataContext, LocalizeValue checkinActionName) {
        List<FilePath> roots = getRootsStream(dataContext).limit(2).collect(Collectors.toList());
        if (!roots.isEmpty()) {
            return roots.get(0).isDirectory()
                ? VcsLocalize.actionCheckInDirectories0Text(checkinActionName, roots.size())
                : VcsLocalize.actionCheckInFiles0Text(checkinActionName, roots.size());
        }

        return checkinActionName;
    }

    
    @Override
    protected LocalizeValue getMnemonicsFreeActionName(VcsContext context) {
        return modifyCheckinActionName(context, VcsLocalize.vcsCommandNameCheckinNoMnemonics());
    }

    @Nullable
    @Override
    protected LocalChangeList getInitiallySelectedChangeList(VcsContext context, Project project) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        LocalChangeList defaultChangeList = manager.getDefaultChangeList();
        LocalChangeList result = null;

        for (FilePath root : getRoots(context)) {
            if (root.getVirtualFile() == null) {
                continue;
            }

            Collection<Change> changes = manager.getChangesIn(root);
            if (defaultChangeList != null && containsAnyChange(defaultChangeList, changes)) {
                return defaultChangeList;
            }
            result = changes.stream().findFirst().map(manager::getChangeList).orElse(null);
        }

        return ObjectUtil.chooseNotNull(result, defaultChangeList);
    }

    @Override
    protected boolean approximatelyHasRoots(VcsContext dataContext) {
        FileStatusManager manager = FileStatusManager.getInstance(dataContext.getProject());

        return getRootsStream(dataContext)
            .map(FilePath::getVirtualFile)
            .filter(Objects::nonNull)
            .anyMatch(file -> isApplicableRoot(file, manager.getStatus(file), dataContext));
    }

    protected boolean isApplicableRoot(VirtualFile file, FileStatus status, VcsContext dataContext) {
        return status != FileStatus.UNKNOWN && status != FileStatus.IGNORED;
    }

    
    @Override
    protected FilePath[] getRoots(VcsContext context) {
        return context.getSelectedFilePaths();
    }

    
    protected Stream<FilePath> getRootsStream(VcsContext context) {
        return context.getSelectedFilePathsStream();
    }

    private static boolean containsAnyChange(LocalChangeList changeList, Collection<Change> changes) {
        return changes.stream().anyMatch(changeList.getChanges()::contains);
    }

    @Nullable
    private static AbstractVcs getCommonVcs(Stream<FilePath> roots, Project project) {
        return Streams.getIfSingle(
            roots.map(root -> VcsUtil.getVcsFor(project, root))
                .filter(Objects::nonNull)
                .distinct()
                .limit(Math.min(2, ProjectLevelVcsManager.getInstance(project).getAllActiveVcss().length))
        );
    }
}
