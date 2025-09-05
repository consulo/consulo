/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.annotation.DeprecationInfo;
import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangeNodeDecorator;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesBrowserNode;
import consulo.versionControlSystem.impl.internal.change.ui.awt.TreeModelBuilder;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

@Deprecated
@DeprecationInfo("Use ChangesBrowserBase due moved to impl")
public class ChangesBrowser extends ChangesBrowserBase<Change> {

    public ChangesBrowser(Project project,
                          List<? extends ChangeList> changeLists,
                          List<Change> changes,
                          ChangeList initialListSelection,
                          boolean capableOfExcludingChanges,
                          boolean highlightProblems,
                          @Nullable Runnable inclusionListener,
                          MyUseCase useCase, @Nullable VirtualFile toSelect) {
        super(project, changes, capableOfExcludingChanges, highlightProblems, inclusionListener, useCase, toSelect, Change.class);

        init();
        setInitialSelection(changeLists, changes, initialListSelection);
        rebuildList();
    }

    @Override
    @Nonnull
    protected DefaultTreeModel buildTreeModel(List<Change> changes, ChangeNodeDecorator changeNodeDecorator, boolean showFlatten) {
        TreeModelBuilder builder = new TreeModelBuilder(myProject, showFlatten);
        return builder.buildModel(changes, changeNodeDecorator);
    }

    @Override
    @Nonnull
    protected List<Change> getSelectedObjects(@Nonnull ChangesBrowserNode<Change> node) {
        return node.getAllChangesUnder();
    }

    @Override
    @Nullable
    protected Change getLeadSelectedObject(@Nonnull ChangesBrowserNode node) {
        Object o = node.getUserObject();
        if (o instanceof Change) {
            return (Change) o;
        }
        return null;
    }

    @Nonnull
    @Override
    public List<Change> getSelectedChanges() {
        return myViewer.getSelectedChanges();
    }

    @Nonnull
    @Override
    public List<Change> getAllChanges() {
        return myViewer.getChanges();
    }

    @Nonnull
    @Override
    public List<Change> getCurrentDisplayedChanges() {
        return myChangesToDisplay != null ? myChangesToDisplay : super.getCurrentDisplayedChanges();
    }

    @Nonnull
    @Override
    public List<Change> getCurrentIncludedChanges() {
        return new ArrayList<>(myViewer.getIncludedChanges());
    }

    @Nonnull
    @Override
    public List<Change> getCurrentDisplayedObjects() {
        return getCurrentDisplayedChanges();
    }
}
