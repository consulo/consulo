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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.Presentation;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.ide.impl.idea.openapi.vcs.actions.AbstractCommonCheckinAction;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public abstract class AbstractCommitChangesAction extends AbstractCommonCheckinAction {
    protected AbstractCommitChangesAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    @Nonnull
    @Override
    protected FilePath[] getRoots(@Nonnull VcsContext context) {
        return getAllContentRoots(context);
    }

    @Override
    protected boolean approximatelyHasRoots(@Nonnull VcsContext dataContext) {
        Project project = dataContext.getProject();
        ProjectLevelVcsManager manager = ProjectLevelVcsManager.getInstance(project);
        return manager.hasAnyMappings();
    }

    protected boolean filterRootsBeforeAction() {
        return false;
    }

    @Override
    protected void update(@Nonnull VcsContext vcsContext, @Nonnull Presentation presentation) {
        super.update(vcsContext, presentation);
        if (presentation.isVisible() && presentation.isEnabled()) {
            ChangeList[] selectedChangeLists = vcsContext.getSelectedChangeLists();
            Change[] selectedChanges = vcsContext.getSelectedChanges();
            if (vcsContext.getPlace().equals(ActionPlaces.CHANGES_VIEW_POPUP)) {
                if (selectedChangeLists != null && selectedChangeLists.length > 0) {
                    presentation.setEnabled(selectedChangeLists.length == 1);
                }
                else {
                    presentation.setEnabled(selectedChanges != null && selectedChanges.length > 0);
                }
            }
            if (presentation.isEnabled() && selectedChanges != null) {
                ChangeListManager changeListManager = ChangeListManager.getInstance(vcsContext.getProject());
                for (Change c : selectedChanges) {
                    if (c.getFileStatus() == FileStatus.HIJACKED && changeListManager.getChangeList(c) == null) {
                        presentation.setEnabled(false);
                        break;
                    }
                }
            }
        }
    }
}