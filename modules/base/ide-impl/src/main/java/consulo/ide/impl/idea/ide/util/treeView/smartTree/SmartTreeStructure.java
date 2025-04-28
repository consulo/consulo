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

package consulo.ide.impl.idea.ide.util.treeView.smartTree;

import consulo.annotation.access.RequiredReadAction;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.fileEditor.structureView.tree.TreeModel;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import jakarta.annotation.Nonnull;

public class SmartTreeStructure extends AbstractTreeStructure {
    protected final TreeModel myModel;
    protected final Project myProject;
    private TreeElementWrapper myRootElementWrapper;

    public SmartTreeStructure(@Nonnull Project project, @Nonnull TreeModel model) {
        myModel = model;
        myProject = project;
    }

    @Override
    public void commit() {
    }

    @Override
    @Nonnull
    public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        return (AbstractTreeNode)element;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Object[] getChildElements(@Nonnull Object element) {
        return ((AbstractTreeNode)element).getChildren().toArray();
    }

    @Override
    public Object getParentElement(@Nonnull Object element) {
        return ((AbstractTreeNode)element).getParent();
    }

    @Nonnull
    @Override
    public Object getRootElement() {
        if (myRootElementWrapper == null) {
            myRootElementWrapper = createTree();
        }
        return myRootElementWrapper;
    }

    protected TreeElementWrapper createTree() {
        return new TreeElementWrapper(myProject, myModel.getRoot(), myModel);
    }

    @Override
    public boolean isAlwaysLeaf(@Nonnull Object element) {
        return ((AbstractTreeNode)element).isAlwaysLeaf();
    }

    @Override
    public boolean hasSomethingToCommit() {
        return PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
    }

    public void rebuildTree() {
        ((CachingChildrenTreeNode)getRootElement()).rebuildChildren();
    }
}
