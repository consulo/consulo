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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.icon.VersionControlSystemIconGroup;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ActionImpl(id = "CheckinProject")
public class CommonCheckinProjectAction extends AbstractCommonCheckinAction {
    @Inject
    public CommonCheckinProjectAction() {
        this(
            ActionLocalize.actionCheckinprojectText(),
            ActionLocalize.actionCheckinprojectText(),
            VersionControlSystemIconGroup.commit()
        );
    }

    protected CommonCheckinProjectAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    @Nonnull
    @Override
    protected FilePath[] getRoots(@Nonnull VcsContext context) {
        Project project = context.getProject();
        List<FilePath> virtualFiles = new ArrayList<>();
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        for (AbstractVcs vcs : vcsManager.getAllActiveVcss()) {
            if (vcs.getCheckinEnvironment() != null) {
                VirtualFile[] roots = vcsManager.getRootsUnderVcs(vcs);
                for (VirtualFile root : roots) {
                    virtualFiles.add(new FilePathImpl(root));
                }
            }
        }
        return virtualFiles.toArray(new FilePath[virtualFiles.size()]);
    }

    @Override
    protected boolean approximatelyHasRoots(@Nonnull VcsContext dataContext) {
        Project project = dataContext.getProject();
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        return vcsManager.hasAnyMappings();
    }

    @Override
    protected void update(@Nonnull VcsContext vcsContext, @Nonnull Presentation presentation) {
        Project project = vcsContext.getProject();
        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        ProjectLevelVcsManager plVcsManager = ProjectLevelVcsManager.getInstance(project);
        if (!plVcsManager.hasActiveVcss()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        String actionName = getActionName(vcsContext) + "...";
        presentation.setText(actionName);

        presentation.setEnabled(!plVcsManager.isBackgroundVcsOperationRunning());
        presentation.setVisible(true);
    }

    @Override
    protected String getActionName(@Nonnull VcsContext dataContext) {
        return VcsLocalize.actionNameCommitProject().get();
    }

    @Override
    protected String getMnemonicsFreeActionName(@Nonnull VcsContext context) {
        return VcsLocalize.vcsCommandNameCheckinNoMnemonics().get();
    }

    protected boolean filterRootsBeforeAction() {
        return false;
    }
}
