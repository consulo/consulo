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
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ProjectViewProjectNode;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.Project;
import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.language.psi.PsiDocumentManager;
import jakarta.annotation.Nonnull;

public abstract class AbstractProjectTreeStructure extends ProjectAbstractTreeStructureBase implements ViewSettings {
    private final AbstractTreeNode myRoot;

    public AbstractProjectTreeStructure(Project project) {
        super(project);
        myRoot = createRoot(project, this);
    }

    protected AbstractTreeNode createRoot(final Project project, ViewSettings settings) {
        return new ProjectViewProjectNode(myProject, this);
    }

    @Nonnull
    @Override
    public <T> T getViewOption(@Nonnull KeyWithDefaultValue<T> option) {
        return option.getDefaultValue();
    }

    @Override
    public abstract boolean isShowMembers();

    @Nonnull
    @Override
    public final Object getRootElement() {
        return myRoot;
    }

    @Override
    public final void commit() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    }

    @Nonnull
    @Override
    public ActionCallback asyncCommit() {
        return PsiDocumentManager.asyncCommitDocuments(myProject);
    }

    @Override
    public final boolean hasSomethingToCommit() {
        return !myProject.isDisposed()
            && PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
    }

    @Override
    public boolean isStructureView() {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(@Nonnull Object element) {
        if (element instanceof ProjectViewNode projectViewNode) {
            return projectViewNode.isAlwaysLeaf();
        }
        return super.isAlwaysLeaf(element);
    }
}