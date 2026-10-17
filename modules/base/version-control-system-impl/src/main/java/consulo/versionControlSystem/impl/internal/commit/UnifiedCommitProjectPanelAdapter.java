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

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitProjectPanelAdapter implements CheckinProjectPanel {
    private final Project myProject;
    private final UnifiedCommitWorkflowUi myUi;
    private final LocalizeValue myCommitActionName;

    public UnifiedCommitProjectPanelAdapter(Project project, UnifiedCommitWorkflowUi ui, LocalizeValue commitActionName) {
        myProject = project;
        myUi = ui;
        myCommitActionName = commitActionName;
    }

    @Override
    public JComponent getComponent() {
        return null;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public boolean hasDiffs() {
        return !myUi.getIncludedChanges().isEmpty() || !myUi.getIncludedUnversionedFiles().isEmpty();
    }

    @Override
    public Collection<VirtualFile> getVirtualFiles() {
        Set<VirtualFile> files = new LinkedHashSet<>();
        for (Change change : myUi.getIncludedChanges()) {
            VirtualFile file = ChangesUtil.getFilePath(change).getVirtualFile();
            if (file != null) {
                files.add(file);
            }
        }
        files.addAll(myUi.getIncludedUnversionedFiles());
        return files;
    }

    @Override
    public Collection<Change> getSelectedChanges() {
        return myUi.getIncludedChanges();
    }

    @Override
    public Collection<File> getFiles() {
        Set<File> files = new LinkedHashSet<>();
        for (Change change : myUi.getIncludedChanges()) {
            files.add(ChangesUtil.getFilePath(change).getIOFile());
        }
        return files;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean vcsIsAffected(String name) {
        for (AbstractVcs vcs : ChangesUtil.getAffectedVcses(myUi.getIncludedChanges(), myProject)) {
            if (vcs.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<VirtualFile> getRoots() {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
        Set<VirtualFile> roots = new LinkedHashSet<>();
        List<Change> changes = myUi.getIncludedChanges();
        for (Change change : changes) {
            FilePath path = ChangesUtil.getFilePath(change);
            VirtualFile root = vcsManager.getVcsRootFor(path);
            if (root != null) {
                roots.add(root);
            }
        }
        return roots;
    }

    @Override
    public String getCommitMessage() {
        return myUi.getCommitMessage();
    }

    @Override
    public void setCommitMessage(String currentDescription) {
        myUi.setCommitMessage(currentDescription);
    }

    @Override
    public boolean isCheckSpelling() {
        return VcsConfiguration.getInstance(myProject).CHECK_COMMIT_MESSAGE_SPELLING;
    }

    @Override
    public void setCheckSpelling(boolean checkSpelling) {
        VcsConfiguration.getInstance(myProject).CHECK_COMMIT_MESSAGE_SPELLING = checkSpelling;
    }

    @Override
    public LocalizeValue getCommitActionName() {
        return myCommitActionName;
    }

    @Override
    public void refresh() {
    }

    @Override
    public void saveState() {
    }

    @Override
    public void restoreState() {
    }
}
